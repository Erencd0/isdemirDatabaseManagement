// Shrinks a photo in the browser BEFORE it is uploaded. A phone photo is several MB and the
// gallery never shows it bigger than a screen, so sending the original only costs upload time
// and bucket space. Web only: the mobile app (Expo) resizes with its own tooling.

const MAX_EDGE = 1600 // px, the long edge of the uploaded photo
const QUALITY = 0.8 // jpeg quality of the re-encoded image
const SKIP_UNDER = 300 * 1024 // bytes, a file this small is not worth touching

// The formats a canvas can decode and re-encode without surprises. Anything else (gif - the
// animation would be lost, svg, a heic the browser cannot decode) is uploaded untouched.
const RESIZABLE = ['image/jpeg', 'image/png', 'image/webp']

export async function shrinkImage(file) {
  if (!RESIZABLE.includes(file.type) || file.size <= SKIP_UNDER) return file
  try {
    // from-image applies the EXIF orientation: the tag does not survive the re-encoding, so a
    // photo taken sideways would be stored rotated without it.
    const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' })
    const scale = Math.min(1, MAX_EDGE / Math.max(bitmap.width, bitmap.height))
    const canvas = document.createElement('canvas')
    canvas.width = Math.round(bitmap.width * scale)
    canvas.height = Math.round(bitmap.height * scale)

    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#fff' // a transparent png would turn black on jpeg without this
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    bitmap.close()

    const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', QUALITY))
    // An already well compressed file can come out bigger than the original: then keep it.
    if (!blob || blob.size >= file.size) return file

    // The name is what the user sees in the gallery and the extension is what the backend puts
    // in the bucket path, so it has to follow the new jpeg content.
    const name = file.name.replace(/\.[^.]+$/, '') + '.jpg'
    return new File([blob], name, { type: 'image/jpeg', lastModified: file.lastModified })
  } catch {
    // Decoding failed (an exotic format): send the original and let the backend decide.
    return file
  }
}
