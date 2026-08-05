import { useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import Button from '../components/Button.jsx'
import Modal from '../components/Modal.jsx'
import { apiFetch, hasSession, logoutRequest, SessionError } from '../api/client.js'

// Every request here goes through apiFetch: it adds the access token as
// "Authorization: Bearer ..." and on a 401 it throws SessionError and redirects to the login
// page. That is why the catch blocks check for SessionError first; in that case there is no
// point in showing a "server error" on screen, the page is already changing.

// The fields of the heat form. malzeme_kodu is excluded (by rule), and
// dokum_id / dokum_no / kayit_zamani / kullanici_id are assigned by the backend.
// The field names must match the backend JSON (camelCase).
const HEAT_FIELDS = [
  { name: 'hurdaSarjBaslamaZamani', label: 'Hurda Şarj Başlama', type: 'datetime-local' },
  { name: 'hurdaSarjBitisZamani', label: 'Hurda Şarj Bitiş', type: 'datetime-local' },
  { name: 'anaUflemeyeBaslamaZamani', label: 'Ana Üflemeye Başlama', type: 'datetime-local' },
  { name: 'anaUflemeBitisZamani', label: 'Ana Üfleme Bitiş', type: 'datetime-local' },
  { name: 'dokumZamani', label: 'Döküm Zamanı', type: 'datetime-local' },
  { name: 'shdSicaklik', label: 'SHD Sıcaklık', type: 'number', min: 1200, max: 1500 },
  { name: 'dokumSicaklik', label: 'Döküm Sıcaklık', type: 'number', min: 1500, max: 1700 },
  {
    name: 'lansSkalDurum',
    label: 'Lans Skal Durum',
    type: 'select',
    required: true,
    // The visible text is AZ SKALLI, the value sent to the DB is AZ_SKALLI
    // (the DB only accepts these 3)
    options: [
      { value: 'AZ_SKALLI', label: 'AZ SKALLI' },
      { value: 'ORTA_SKALLI', label: 'ORTA SKALLI' },
      { value: 'COK_SKALLI', label: 'ÇOK SKALLI' },
    ],
  },
]

const EMPTY_FORM = HEAT_FIELDS.reduce((acc, f) => ({ ...acc, [f.name]: '' }), {})

// Adding materials (steps 4-5-6). 3 additive buttons, mapped to the malzeme_turu values in
// the DB. Pressing a button loads the materials of that type into the combobox through
// /api/malzeme?tur=...
const ADDITIVES = [
  { type: 'KONVKATKI', label: 'Konverter Katkı' },
  { type: 'POTAKATKI', label: 'Pota Katkı' },
  { type: 'HURDAKATKI', label: 'Hurda Katkı' },
]

const EMPTY_MATERIAL_FORM = { malzemeKodu: '', miktar: '', malzemeVerilisTarihi: '' }

// Additive type code -> visible label (used in the detail and in the list)
const ADDITIVE_LABELS = ADDITIVES.reduce((acc, a) => ({ ...acc, [a.type]: a.label }), {})

// Field name -> visible label (used in the time validation messages)
const FIELD_LABELS = HEAT_FIELDS.reduce((acc, f) => ({ ...acc, [f.name]: f.label }), {})

// The chronological order of the process times. None of them may come BEFORE the previous
// one: scrap charge start -> end -> main blow start -> main blow end -> tap
const TIME_ORDER = [
  'hurdaSarjBaslamaZamani',
  'hurdaSarjBitisZamani',
  'anaUflemeyeBaslamaZamani',
  'anaUflemeBitisZamani',
  'dokumZamani',
]

// Makes a datetime-local value readable ("2026-08-02T14:30" -> "02.08.2026 14:30")
function formatDate(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (isNaN(d)) return value
  return d.toLocaleString('tr-TR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function Dashboard() {
  const navigate = useNavigate()

  const username = localStorage.getItem('kullanici_adi')
  const selectedRole = localStorage.getItem('secili_rol')
  const userId = localStorage.getItem('kullanici_id')

  // Selected role (kv1/kv2/kv3) -> converter number (1/2/3). This is the 2nd digit of dokum_no.
  const converterNo = Number((selectedRole || '').replace(/[^0-9]/g, ''))

  // The heat form
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [formMessage, setFormMessage] = useState(null) // only the save result (ok/server error)
  const [errors, setErrors] = useState({}) // per field validation errors: { fieldName: message }

  // The saved heat (when there is one). It is kept here after Kaydet and the materials are
  // added to it. While it is set the form is locked (a second Kaydet must not create a
  // duplicate); "Yeni Döküm" clears it and moves on to the next heat.
  const [savedHeat, setSavedHeat] = useState(null)
  const saved = savedHeat !== null

  // The dokum_no this user will get once saved (calculated by the backend, shown read only)
  const [nextNo, setNextNo] = useState(null)

  // The "list the heats" popup
  const [listOpen, setListOpen] = useState(false)
  const [heats, setHeats] = useState([])
  const [listLoading, setListLoading] = useState(false)
  const [listError, setListError] = useState('')

  // --- Adding materials (steps 4-5-6) ---
  // The selected additive (KONVKATKI/POTAKATKI/HURDAKATKI). The material form does not open
  // before one of the buttons is pressed.
  const [selectedAdditive, setSelectedAdditive] = useState(null)
  const [materialOptions, setMaterialOptions] = useState([]) // combobox of the selected additive
  const [materialsLoading, setMaterialsLoading] = useState(false)
  const [materialForm, setMaterialForm] = useState(EMPTY_MATERIAL_FORM)
  const [materialErrors, setMaterialErrors] = useState({})
  const [materialMessage, setMaterialMessage] = useState(null)
  const [materialSaving, setMaterialSaving] = useState(false)
  // The materials added to this heat (shown in the list after saving). The rows also keep
  // _type and malzemeAdi (on the client side) for the list and the edit row.
  const [addedMaterials, setAddedMaterials] = useState([])
  // The row being edited (when set, the inline edit row is rendered)
  const [editingRow, setEditingRow] = useState(null)

  // Fetches the next dokum_no of this user from the backend
  const fetchNextNo = async () => {
    try {
      const res = await apiFetch(`/api/dokum/sonraki-no?konverterNo=${converterNo}`)
      if (res.ok) setNextNo(await res.json())
    } catch {
      // ignore silently, the form still works without the number
    }
  }

  // Fetch the next dokum_no on mount
  useEffect(() => {
    fetchNextNo()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // No session (token + role) -> back to login
  if (!hasSession()) {
    return <Navigate to="/login" replace />
  }

  // Logout: tell the backend first (the refresh token is set to aktif = false),
  // then clear the local session and go back to login.
  const logout = async () => {
    await logoutRequest()
    navigate('/login', { replace: true })
  }

  const changeField = (name, value) => {
    setForm((previous) => ({ ...previous, [name]: value }))
    // Clear the error of a field while the user is fixing it
    setErrors((e) => {
      if (!e[name]) return e
      const rest = { ...e }
      delete rest[name]
      return rest
    })
  }

  // Finishes the current heat completely and moves to the next one: clears the form, drops
  // the saved heat and refreshes the next dokum_no.
  const newHeat = () => {
    setForm(EMPTY_FORM)
    setFormMessage(null)
    setErrors({})
    setSavedHeat(null)
    // Reset the material section too: new heat, new material list
    setSelectedAdditive(null)
    setMaterialOptions([])
    setMaterialForm(EMPTY_MATERIAL_FORM)
    setMaterialErrors({})
    setMaterialMessage(null)
    setAddedMaterials([])
    fetchNextNo()
  }

  // Refresh: fetches the heat being worked on from the backend again and refreshes the form
  // plus the material list (changes made from Postman/the automation show up here).
  // IMPORTANT: it does NOT ask for the next dokum_no again; it stays on the same number, so
  // it does not "jump" to another heat while the automation keeps adding new heats.
  //  - With a loaded heat it is refreshed by dokum_id.
  //  - Without one, it loads the record created (by the event flow) for the dokum_no on
  //    screen; when there is none yet it shows an info message (the user is still creating it).
  const refresh = async () => {
    if (saved) {
      showDetail(savedHeat.dokumId)
      return
    }
    if (nextNo == null) return
    try {
      const res = await apiFetch(`/api/dokum/no/${nextNo}`)
      if (res.ok) {
        const { dokum, malzemeler } = await res.json()
        loadHeatIntoForm(dokum, malzemeler)
      } else {
        setFormMessage({ type: 'error', text: `Döküm No: ${nextNo} için henüz kayıt yok.` })
      }
    } catch (e) {
      if (e instanceof SessionError) return
      setFormMessage({ type: 'error', text: 'Sunucuya bağlanılamadı' })
    }
  }

  // STEP 4: pressing an additive button loads the materials of that type into the combobox
  const selectAdditive = async (type) => {
    setSelectedAdditive(type)
    setMaterialForm((f) => ({ ...f, malzemeKodu: '' })) // the selection resets with the additive
    setMaterialErrors({})
    setMaterialMessage(null)
    setMaterialsLoading(true)
    try {
      const res = await apiFetch(`/api/malzeme?tur=${type}`)
      setMaterialOptions(res.ok ? await res.json() : [])
    } catch {
      // SessionError included: the options stay empty, on a 401 we are heading to login anyway
      setMaterialOptions([])
    } finally {
      setMaterialsLoading(false)
    }
  }

  const changeMaterialField = (name, value) => {
    setMaterialForm((f) => ({ ...f, [name]: value }))
    setMaterialErrors((e) => {
      if (!e[name]) return e
      const rest = { ...e }
      delete rest[name]
      return rest
    })
  }

  // STEP 5: adds the selected material to the current heat and puts it into the list below
  const addMaterial = async (e) => {
    e.preventDefault()
    if (!saved) return
    setMaterialMessage(null)

    const newErrors = {}
    if (!materialForm.malzemeKodu) newErrors.malzemeKodu = 'Malzeme seçiniz'
    if (materialForm.miktar === '') newErrors.miktar = 'Miktar giriniz'
    else if (Number(materialForm.miktar) <= 0) newErrors.miktar = "Miktar 0'dan büyük olmalı"
    if (!materialForm.malzemeVerilisTarihi) newErrors.malzemeVerilisTarihi = 'Tarih giriniz'
    if (Object.keys(newErrors).length > 0) {
      setMaterialErrors(newErrors)
      return
    }
    setMaterialErrors({})

    setMaterialSaving(true)
    try {
      const payload = {
        malzemeKodu: Number(materialForm.malzemeKodu),
        miktar: Number(materialForm.miktar),
        malzemeVerilisTarihi: materialForm.malzemeVerilisTarihi,
        kullaniciId: Number(userId),
      }
      const res = await apiFetch(`/api/dokum/${savedHeat.dokumId}/malzeme`, {
        method: 'POST',
        body: payload,
      })
      if (!res.ok) {
        // Show the backend validation error (400); fall back to a generic message
        const text = await res.text().catch(() => '')
        setMaterialMessage({ type: 'error', text: text || 'Malzeme eklenemedi' })
        return
      }
      const savedMaterial = await res.json()
      // The backend does not fill malzeme_adi here; we take the name and type from the
      // selected combobox entry and put them on the row
      const name = materialOptions.find((m) => m.malzemeKodu === payload.malzemeKodu)?.malzemeAdi
      setAddedMaterials((prev) => [
        ...prev,
        { ...savedMaterial, malzemeAdi: name, _type: selectedAdditive },
      ])
      // Quantity and date are cleared, the additive stays selected (easy consecutive adds)
      setMaterialForm((f) => ({ ...f, malzemeKodu: '', miktar: '', malzemeVerilisTarihi: '' }))
      setMaterialMessage({ type: 'ok', text: 'Malzeme eklendi' })
    } catch (e) {
      if (e instanceof SessionError) return
      setMaterialMessage({ type: 'error', text: 'Sunucuya bağlanılamadı' })
    } finally {
      setMaterialSaving(false)
    }
  }

  // STEP 6: deletes a material from the list
  const deleteMaterial = async (usageId) => {
    try {
      const res = await apiFetch(`/api/dokum/${savedHeat.dokumId}/malzeme/${usageId}`, {
        method: 'DELETE',
      })
      if (res.ok) {
        setAddedMaterials((prev) => prev.filter((m) => m.kullanimId !== usageId))
      }
    } catch {
      // ignore silently; the list stays as it is when the delete fails
    }
  }

  // Updates the row in the list once the edit row saves
  const materialUpdated = (updated) => {
    setAddedMaterials((prev) =>
      prev.map((m) => (m.kullanimId === updated.kullanimId ? updated : m)),
    )
    setEditingRow(null)
  }

  const saveHeat = async (e) => {
    e.preventDefault()
    // Do not save again when it is already saved (the backend would create a duplicate)
    if (saved) return
    setFormMessage(null)

    // Per field validation: cannot be empty + temperature range. The errors are shown under
    // each field.
    const newErrors = {}
    for (const field of HEAT_FIELDS) {
      const value = form[field.name]
      if (value === '' || value == null) {
        newErrors[field.name] = 'Bu alan boş bırakılamaz'
        continue
      }
      if (field.type === 'number' && field.min != null) {
        const number = Number(value)
        if (number < field.min || number > field.max) {
          newErrors[field.name] = `${field.min}-${field.max} aralığında olmalı`
        }
      }
    }

    // Time ordering: no time may come before the process time preceding it.
    // Empty fields already got an error above, so they are skipped here.
    for (let i = 1; i < TIME_ORDER.length; i++) {
      const previousName = TIME_ORDER[i - 1]
      const currentName = TIME_ORDER[i]
      const previous = form[previousName]
      const current = form[currentName]
      if (previous && current && !newErrors[currentName] && new Date(current) < new Date(previous)) {
        newErrors[currentName] = `${FIELD_LABELS[previousName]} zamanından önce olamaz`
      }
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors)
      return
    }
    setErrors({})

    setSaving(true)
    try {
      // Empty fields are sent as null, numbers are converted with Number().
      // converterNo comes from the selected role (it is not on the form) and dokum_no is
      // derived from it.
      const payload = { kullaniciId: Number(userId), konverterNo: converterNo }
      for (const field of HEAT_FIELDS) {
        const value = form[field.name]
        if (value === '' || value == null) {
          payload[field.name] = null
        } else if (field.type === 'number') {
          payload[field.name] = Number(value)
        } else {
          payload[field.name] = value
        }
      }

      const res = await apiFetch('/api/dokum', {
        method: 'POST',
        body: payload,
      })
      if (!res.ok) {
        // Show the backend validation error (400); fall back to a generic message
        const text = await res.text().catch(() => '')
        setFormMessage({
          type: 'error',
          text: text || 'Döküm kaydedilemedi (sunucu hatası)',
        })
        return
      }
      const savedRecord = await res.json()
      // The heat is saved but the form is untouched and the number does NOT advance: this
      // heat still needs its materials. The form locks; "Yeni Döküm" moves to the next heat.
      setSavedHeat(savedRecord)
      setFormMessage({
        type: 'ok',
        text: `Döküm kaydedildi (No: ${savedRecord.dokumNo ?? savedRecord.dokumId}). Malzeme ekleyebilir veya "Yeni Döküm" ile sonraki döküme geçebilirsiniz.`,
      })
    } catch (e) {
      if (e instanceof SessionError) return
      setFormMessage({ type: 'error', text: 'Sunucuya bağlanılamadı' })
    } finally {
      setSaving(false)
    }
  }

  const listHeats = async () => {
    setListOpen(true)
    setListError('')
    setListLoading(true)
    try {
      const res = await apiFetch('/api/dokum')
      if (!res.ok) {
        setListError('Dökümler alınamadı')
        return
      }
      setHeats(await res.json())
    } catch (e) {
      if (e instanceof SessionError) return
      setListError('Sunucuya bağlanılamadı')
    } finally {
      setListLoading(false)
    }
  }

  // Fills a heat + its materials into the main form and the list below; puts the heat into
  // "saved" mode (form locked, No visible, open for adding materials). Shared by showDetail
  // and refresh.
  const loadHeatIntoForm = (heat, materials) => {
    // Fill the heat fields into the form. datetime-local expects "YYYY-MM-DDTHH:mm", so the
    // first 16 characters of the backend ISO string are enough. The rest becomes a string.
    const newForm = {}
    for (const field of HEAT_FIELDS) {
      const value = heat[field.name]
      if (value == null) {
        newForm[field.name] = ''
      } else if (field.type === 'datetime-local') {
        newForm[field.name] = String(value).slice(0, 16)
      } else {
        newForm[field.name] = String(value)
      }
    }
    setForm(newForm)
    setErrors({})
    setFormMessage({
      type: 'ok',
      text: `Döküm No: ${heat.dokumNo} yüklendi. Malzemeleri aşağıda görebilir, ekleme/düzenleme yapabilirsiniz.`,
    })
    setSavedHeat(heat)

    // Put the materials into the list below (the detail sends the type as "malzemeTuru",
    // the list expects "_type")
    setAddedMaterials(materials.map((m) => ({ ...m, _type: m.malzemeTuru })))

    // Reset the material form and close the list
    setSelectedAdditive(null)
    setMaterialOptions([])
    setMaterialForm(EMPTY_MATERIAL_FORM)
    setMaterialErrors({})
    setMaterialMessage(null)
    setListOpen(false)
  }

  // "Detay Gör": fills the heat into the fields of the main form (instead of a popup) and
  // its materials into the list below, looked up by dokum_id.
  const showDetail = async (id) => {
    try {
      const res = await apiFetch(`/api/dokum/${id}`)
      if (!res.ok) return
      const { dokum, malzemeler } = await res.json()
      loadHeatIntoForm(dokum, malzemeler)
    } catch {
      // ignore silently, the detail cannot be loaded
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* STEP 1: top bar - the logged in user + logout */}
      <header className="bg-brand-700 text-white shadow">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-8 py-4">
          <h1 className="text-xl font-bold">İsdemir Döküm Paneli</h1>
          <div className="flex items-center gap-4">
            <div className="text-right leading-tight">
              <p className="font-semibold">{username}</p>
              <p className="text-sm text-brand-100">Rol: {selectedRole}</p>
            </div>
            <Button variant="secondary" size="sm" onClick={logout}>
              Çıkış Yap
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-8 py-6">
        {/* STEPS 2 + 3: the buttons + the always visible heat form */}
        <div className="mb-4 flex gap-3">
          <Button onClick={listHeats}>Dökümleri Listele</Button>
          <Button variant="outline" onClick={newHeat}>
            Yeni Döküm
          </Button>
          <Button variant="outline" onClick={refresh} disabled={nextNo == null}>
            Yenile
          </Button>
        </div>

        <section className="rounded-2xl bg-white p-6 shadow">
          <h2 className="mb-4 text-lg font-bold text-brand-700">Yeni Döküm Ekle</h2>

          {/* The dokum_no that will be saved - read only, for information */}
          <div className="mb-4 flex items-center gap-3 rounded-lg bg-brand-50 px-4 py-3">
            <span className="text-sm font-medium text-gray-600">Döküm No:</span>
            <span className="text-lg font-bold text-brand-700">
              {saved ? savedHeat.dokumNo : (nextNo ?? '...')}
            </span>
          </div>

          <form onSubmit={saveHeat}>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {HEAT_FIELDS.map((field) => (
                <div key={field.name}>
                  <label className="mb-1 block text-sm font-medium text-gray-700">
                    {field.label}
                    <span className="ml-0.5 text-red-500">*</span>
                    {field.min != null && (
                      <span className="ml-1 text-xs font-normal text-gray-400">
                        ({field.min}-{field.max})
                      </span>
                    )}
                  </label>
                  {field.type === 'select' ? (
                    <select
                      value={form[field.name]}
                      disabled={saved}
                      onChange={(e) => changeField(field.name, e.target.value)}
                      className={
                        'w-full rounded-lg border bg-white px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100 disabled:text-gray-500 ' +
                        (errors[field.name] ? 'border-red-400' : 'border-gray-300')
                      }
                    >
                      <option value="">Seçiniz</option>
                      {field.options.map((o) => (
                        <option key={o.value} value={o.value}>
                          {o.label}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <input
                      type={field.type}
                      value={form[field.name]}
                      min={field.min}
                      max={field.max}
                      disabled={saved}
                      onChange={(e) => changeField(field.name, e.target.value)}
                      className={
                        'w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100 disabled:text-gray-500 ' +
                        (errors[field.name] ? 'border-red-400' : 'border-gray-300')
                      }
                    />
                  )}
                  {errors[field.name] && (
                    <p className="mt-1 text-xs text-red-600">{errors[field.name]}</p>
                  )}
                </div>
              ))}
            </div>

            {formMessage && (
              <p
                className={
                  'mt-4 rounded-lg border px-3 py-2 text-sm ' +
                  (formMessage.type === 'ok'
                    ? 'border-green-200 bg-green-50 text-green-700'
                    : 'border-red-200 bg-red-50 text-red-600')
                }
              >
                {formMessage.text}
              </p>
            )}

            <div className="mt-4">
              <Button type="submit" disabled={saving || saved}>
                {saving ? 'Kaydediliyor...' : saved ? 'Kaydedildi ✓' : 'Kaydet'}
              </Button>
            </div>
          </form>
        </section>

        {/* STEPS 4-5-6: the material section (active once the heat is saved) */}
        <section className="mt-6 rounded-2xl bg-white p-6 shadow">
          <h2 className="mb-1 text-lg font-bold text-brand-700">Malzeme Ekle</h2>
          {!saved && (
            <p className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">
              Malzeme ekleyebilmek için önce yukarıdan dökümü kaydedin.
            </p>
          )}

          {/* STEP 4: the additive buttons - the additive is picked first, the materials follow */}
          <div className="mb-4 flex flex-wrap gap-3">
            {ADDITIVES.map((a) => (
              <Button
                key={a.type}
                variant={selectedAdditive === a.type ? 'primary' : 'outline'}
                disabled={!saved}
                onClick={() => selectAdditive(a.type)}
              >
                {a.label}
              </Button>
            ))}
          </div>

          {/* STEP 5: the material details of the selected additive are entered */}
          {saved && selectedAdditive && (
            <form onSubmit={addMaterial} className="mb-6">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <div className="sm:col-span-3">
                  <label className="mb-1 block text-sm font-medium text-gray-700">
                    Malzeme<span className="ml-0.5 text-red-500">*</span>
                  </label>
                  <select
                    value={materialForm.malzemeKodu}
                    disabled={materialsLoading}
                    onChange={(e) => changeMaterialField('malzemeKodu', e.target.value)}
                    className={
                      'w-full rounded-lg border bg-white px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100 ' +
                      (materialErrors.malzemeKodu ? 'border-red-400' : 'border-gray-300')
                    }
                  >
                    <option value="">{materialsLoading ? 'Yükleniyor...' : 'Seçiniz'}</option>
                    {materialOptions.map((m) => (
                      <option key={m.malzemeKodu} value={m.malzemeKodu}>
                        {m.malzemeKodu} - {m.malzemeAdi}
                      </option>
                    ))}
                  </select>
                  {materialErrors.malzemeKodu && (
                    <p className="mt-1 text-xs text-red-600">{materialErrors.malzemeKodu}</p>
                  )}
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-gray-700">
                    Miktar (kg)<span className="ml-0.5 text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    min={1}
                    value={materialForm.miktar}
                    onChange={(e) => changeMaterialField('miktar', e.target.value)}
                    className={
                      'w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500 ' +
                      (materialErrors.miktar ? 'border-red-400' : 'border-gray-300')
                    }
                  />
                  {materialErrors.miktar && (
                    <p className="mt-1 text-xs text-red-600">{materialErrors.miktar}</p>
                  )}
                </div>

                <div className="sm:col-span-2">
                  <label className="mb-1 block text-sm font-medium text-gray-700">
                    Veriliş Tarihi<span className="ml-0.5 text-red-500">*</span>
                  </label>
                  <input
                    type="datetime-local"
                    value={materialForm.malzemeVerilisTarihi}
                    onChange={(e) => changeMaterialField('malzemeVerilisTarihi', e.target.value)}
                    className={
                      'w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500 ' +
                      (materialErrors.malzemeVerilisTarihi ? 'border-red-400' : 'border-gray-300')
                    }
                  />
                  {materialErrors.malzemeVerilisTarihi && (
                    <p className="mt-1 text-xs text-red-600">{materialErrors.malzemeVerilisTarihi}</p>
                  )}
                </div>
              </div>

              {materialMessage && (
                <p
                  className={
                    'mt-4 rounded-lg border px-3 py-2 text-sm ' +
                    (materialMessage.type === 'ok'
                      ? 'border-green-200 bg-green-50 text-green-700'
                      : 'border-red-200 bg-red-50 text-red-600')
                  }
                >
                  {materialMessage.text}
                </p>
              )}

              <div className="mt-4">
                <Button type="submit" disabled={materialSaving}>
                  {materialSaving ? 'Ekleniyor...' : 'Malzeme Ekle'}
                </Button>
              </div>
            </form>
          )}

          {/* STEPS 5-6: the list of materials added to this heat + delete/update */}
          <div>
            <h3 className="mb-2 text-sm font-bold text-gray-700">
              Eklenen Malzemeler ({addedMaterials.length})
            </h3>
            {addedMaterials.length === 0 ? (
              <p className="text-sm text-gray-500">Henüz malzeme eklenmedi.</p>
            ) : (
              <table className="w-full text-left text-sm">
                <thead className="text-gray-500">
                  <tr className="border-b border-gray-200">
                    <th className="py-2">Katkı</th>
                    <th className="py-2">Kod</th>
                    <th className="py-2">Malzeme Adı</th>
                    <th className="py-2">Miktar (kg)</th>
                    <th className="py-2">Veriliş Tarihi</th>
                    <th className="py-2 text-right">İşlem</th>
                  </tr>
                </thead>
                <tbody>
                  {addedMaterials.map((m) =>
                    editingRow?.kullanimId === m.kullanimId ? (
                      <MaterialRowEdit
                        key={m.kullanimId}
                        row={m}
                        heatId={savedHeat.dokumId}
                        userId={userId}
                        onCancel={() => setEditingRow(null)}
                        onSaved={materialUpdated}
                      />
                    ) : (
                      <tr key={m.kullanimId} className="border-b border-gray-100">
                        <td className="py-2">{ADDITIVE_LABELS[m._type] ?? m._type ?? '-'}</td>
                        <td className="py-2">{m.malzemeKodu}</td>
                        <td className="py-2">{m.malzemeAdi ?? '-'}</td>
                        <td className="py-2">{m.miktar}</td>
                        <td className="py-2">{formatDate(m.malzemeVerilisTarihi)}</td>
                        <td className="py-2">
                          <div className="flex justify-end gap-2">
                            <Button size="sm" variant="outline" onClick={() => setEditingRow(m)}>
                              Güncelle
                            </Button>
                            <Button size="sm" variant="secondary" onClick={() => deleteMaterial(m.kullanimId)}>
                              Sil
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ),
                  )}
                </tbody>
              </table>
            )}
          </div>
        </section>
      </main>

      {/* STEP 3: the "list the heats" popup */}
      <Modal
        open={listOpen}
        onClose={() => setListOpen(false)}
        title="Dökümler"
        maxWidth="max-w-2xl"
      >
        {listLoading && <p className="text-gray-500">Yükleniyor...</p>}
        {listError && <p className="text-red-600">{listError}</p>}
        {!listLoading && !listError && heats.length === 0 && (
          <p className="text-gray-500">Kayıtlı döküm yok.</p>
        )}
        {!listLoading && heats.length > 0 && (
          <ul className="divide-y divide-gray-100">
            {heats.map((h) => (
              <li key={h.dokumId} className="flex items-center justify-between py-3">
                <div className="text-sm">
                  <p className="font-semibold text-gray-800">Döküm No: {h.dokumNo}</p>
                  <p className="text-gray-500">
                    {formatDate(h.dokumZamani)} · Sıcaklık: {h.dokumSicaklik ?? '-'}
                  </p>
                </div>
                <Button size="sm" variant="outline" onClick={() => showDetail(h.dokumId)}>
                  Detay Gör
                </Button>
              </li>
            ))}
          </ul>
        )}
      </Modal>
    </div>
  )
}

// STEP 6: updating a material - done inline, ON the row itself in the list.
// The edited row is rendered as a <tr> whose cells turn into inputs/selects.
// It loads the materials of the additive the row belongs to (_type), prefills the current
// values and updates them with PUT.
function MaterialRowEdit({ row, heatId, userId, onCancel, onSaved }) {
  const [type, setType] = useState(row._type ?? '')
  const [options, setOptions] = useState([])
  const [loading, setLoading] = useState(false)
  const [form, setForm] = useState({
    malzemeKodu: String(row.malzemeKodu ?? ''),
    miktar: String(row.miktar ?? ''),
    // datetime-local expects "YYYY-MM-DDTHH:mm"; the first 16 characters of the backend ISO
    // string are enough
    malzemeVerilisTarihi: row.malzemeVerilisTarihi
      ? row.malzemeVerilisTarihi.slice(0, 16)
      : '',
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  // When the additive changes, load its materials into the combobox (also runs on mount)
  useEffect(() => {
    if (!type) return
    let cancelled = false
    setLoading(true)
    apiFetch(`/api/malzeme?tur=${type}`)
      .then((r) => (r.ok ? r.json() : []))
      .then((d) => {
        if (!cancelled) setOptions(d)
      })
      .catch(() => {
        if (!cancelled) setOptions([])
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [type])

  const save = async () => {
    setError('')
    if (!form.malzemeKodu || form.miktar === '' || !form.malzemeVerilisTarihi) {
      setError('Tüm alanları doldurun')
      return
    }
    if (Number(form.miktar) <= 0) {
      setError("Miktar 0'dan büyük olmalı")
      return
    }
    setSaving(true)
    try {
      const payload = {
        malzemeKodu: Number(form.malzemeKodu),
        miktar: Number(form.miktar),
        malzemeVerilisTarihi: form.malzemeVerilisTarihi,
        kullaniciId: Number(userId),
      }
      const res = await apiFetch(`/api/dokum/${heatId}/malzeme/${row.kullanimId}`, {
        method: 'PUT',
        body: payload,
      })
      if (!res.ok) {
        // Show the backend validation error (400); fall back to a generic message
        const text = await res.text().catch(() => '')
        setError(text || 'Güncellenemedi')
        return
      }
      const updated = await res.json()
      const name =
        options.find((m) => m.malzemeKodu === payload.malzemeKodu)?.malzemeAdi ??
        row.malzemeAdi
      onSaved({ ...updated, malzemeAdi: name, _type: type })
    } catch (e) {
      if (e instanceof SessionError) return
      setError('Sunucuya bağlanılamadı')
    } finally {
      setSaving(false)
    }
  }

  // The Malzeme Adı cell shows the name of the selected material (updated with the combobox)
  const selectedName =
    options.find((m) => String(m.malzemeKodu) === form.malzemeKodu)?.malzemeAdi ??
    row.malzemeAdi ??
    '-'

  const cellInput =
    'w-full rounded-lg border border-gray-300 bg-white px-2 py-1 focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100'

  return (
    <>
      <tr className="border-b border-gray-100 bg-brand-50/50 align-top">
        <td className="py-2 pr-2">
          <select
            value={type}
            onChange={(e) => {
              setType(e.target.value)
              setForm((f) => ({ ...f, malzemeKodu: '' }))
            }}
            className={cellInput}
          >
            {ADDITIVES.map((a) => (
              <option key={a.type} value={a.type}>
                {a.label}
              </option>
            ))}
          </select>
        </td>
        <td className="py-2 pr-2">
          <select
            value={form.malzemeKodu}
            disabled={loading}
            onChange={(e) => setForm((f) => ({ ...f, malzemeKodu: e.target.value }))}
            className={cellInput}
          >
            <option value="">{loading ? 'Yükleniyor...' : 'Seçiniz'}</option>
            {options.map((m) => (
              <option key={m.malzemeKodu} value={m.malzemeKodu}>
                {m.malzemeKodu} - {m.malzemeAdi}
              </option>
            ))}
          </select>
        </td>
        <td className="py-2 pr-2 text-gray-500">{selectedName}</td>
        <td className="py-2 pr-2">
          <input
            type="number"
            min={1}
            value={form.miktar}
            onChange={(e) => setForm((f) => ({ ...f, miktar: e.target.value }))}
            className={cellInput}
          />
        </td>
        <td className="py-2 pr-2">
          <input
            type="datetime-local"
            value={form.malzemeVerilisTarihi}
            onChange={(e) => setForm((f) => ({ ...f, malzemeVerilisTarihi: e.target.value }))}
            className={cellInput}
          />
        </td>
        <td className="py-2">
          <div className="flex justify-end gap-2">
            <Button size="sm" onClick={save} disabled={saving}>
              {saving ? '...' : 'Kaydet'}
            </Button>
            <Button size="sm" variant="outline" onClick={onCancel} disabled={saving}>
              İptal
            </Button>
          </div>
        </td>
      </tr>
      {error && (
        <tr>
          <td colSpan={6} className="pb-2 text-xs text-red-600">
            {error}
          </td>
        </tr>
      )}
    </>
  )
}

export default Dashboard
