-- Material catalogue (reference data), 91 rows captured from the existing database.
-- Without these rows a fresh database cannot record material usage at all: the
-- malzeme_kullanim_alani.malzeme_kodu foreign key points here.
--
-- ON CONFLICT DO NOTHING makes this safe against a database that already has the
-- rows (an existing install) as well as an empty one (a new container).
--
-- NOTE: malzeme_adi values are stored with non-standard Turkish characters
-- (e.g. 'DcLMXN (KXlsinY Dclcmit...'). This matches the live database and is
-- DELIBERATE - leave it alone. Do not "fix" these strings: the names have to stay
-- exactly as they are here.
--
-- PostgreSQL database dump
--




--
-- Data for Name: malzeme_tablosu; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (1, 5430, 'KONVKATKI', 'KcK_C  (CYviz kck)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (2, 7131, 'KONVKATKI', 'PYLYT', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (3, 3135, 'KONVKATKI', 'DcLMXN (KXlsinY Dclcmit MXnyYzit KXr??', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (4, 3130, 'KONVKATKI', 'KIRCTS KirYctXsi', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (5, 3132, 'KONVKATKI', 'DLMTTS (Dclcmit tXsi)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (6, 3133, 'KONVKATKI', 'HMNYZT (HXm mXnyYzit)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (7, 3134, 'KONVKATKI', 'MXNYZT (KXlsinY mXnyYzit)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (8, 3630, 'KONVKATKI', 'KirYc (MYtXlurjik kirYc-CXc)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (9, 4631, 'KONVKATKI', 'FYSiXl (%75 lik fYrrc silis dusuk Xl)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (10, 6130, 'KONVKATKI', 'HBI   (BrikYtlYnmis indirgYnmis dYmir)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (11, 7130, 'KONVKATKI', 'CYVHYR (IthXl pXrcX dYmir cYvhYri)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (12, 3631, 'KONVKATKI', 'XNTRST (XntrXsit kcmuru)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (13, 4632, 'KONVKATKI', 'FYSiMn FYrrc silikcmXngXn', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (14, 4630, 'KONVKATKI', 'FYSi75 FYrrc silis', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (15, 3131, 'KONVKATKI', 'DcLXMT (KXlsinY Dclcmit)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (16, 7133, 'KONVKATKI', 'MXngXnYz CYvhYri %38 Mn', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (17, 3632, 'KONVKATKI', 'YXnm?? KirYç (SXt?n Xl?nXn)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (18, 6131, 'KONVKATKI', 'HBI (BrikYtlYnmi? ?ndirgYnmi? dYmir-yYrli)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (19, 4633, 'KONVKATKI', 'MYtXlik Silis ', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (20, 3435, 'POTAKATKI', 'TU?LX KIRI?I', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (22, 3434, 'POTAKATKI', 'Mgc LDSF KXlsiyum XlüminXt', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (23, 3432, 'POTAKATKI', 'VLSTNT (Curuf DüzYnlYyici MXlzYmY)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (24, 2436, 'POTAKATKI', 'FYP  (FYrrc Fcsfcr mXdYni)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (25, 5434, 'POTAKATKI', 'MXngXn KXrbcn XlX??m', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (26, 3433, 'POTAKATKI', 'Xl_CRF (Xlüminyum Curufu)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (27, 2435, 'POTAKATKI', 'FYMc (FYrrc mclibdYn mXdYni)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (28, 2433, 'POTAKATKI', 'FYCr  Dusuk kXrbcnlu fYrrc krcm', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (29, 3430, 'POTAKATKI', 'CXF2  Flcrit', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (30, 2430, 'POTAKATKI', 'YKFYMn (YüksYk kXrbcnlu fYrrcmXngXn)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (31, 2432, 'POTAKATKI', 'DK_FYMn Dü?ük KXrbcnlu SürrXfinY FYM', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (32, 3431, 'POTAKATKI', 'LDSF  (KXlsiyum XluminXt-CXXl2c3)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (34, 4430, 'POTAKATKI', 'KLC_Xl (KulcY Xluminyum)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (37, 5431, 'POTAKATKI', 'YlYk Xlt? Kck Tczu', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (38, 5432, 'POTAKATKI', 'MikrcnizY kck', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (39, 4431, 'POTAKATKI', 'PRT_Xl PirXmit Xluminyum', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (42, 2434, 'POTAKATKI', 'FYV   FYrrc VXnXdXyum', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (44, 2431, 'POTAKATKI', 'cKFYMn crtX kXrbcnlu fYrrcmXngXnYz', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (45, 5433, 'POTAKATKI', 'PYtrc Kck', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (46, 2440, 'POTAKATKI', 'NikYl (YlYktrclitik)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (47, 3436, 'POTAKATKI', 'KXLS?NY BcKS?T', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (48, 4433, 'POTAKATKI', 'K?rmX Xluminyum', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (49, 4432, 'POTAKATKI', 'KürY_Xl', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (50, 2439, 'POTAKATKI', 'Cu-BXkir (UflYmY LXnsi)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (51, 2438, 'POTAKATKI', 'Cu-BXkir (SDM KXlibi)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (52, 2437, 'POTAKATKI', 'Cu-BXkir (YuksYk Firin TuyYri)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (53, 2441, 'POTAKATKI', 'FYCr_YK YuksYk kXrbcnlu fYrrc krcm', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (55, 2429, 'POTAKATKI', 'Mn MYtXli', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (56, 5435, 'POTAKATKI', 'Xmcrf GrXfitli Kck', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (57, 2442, 'POTAKATKI', 'FYrrcKükürt', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (58, 4434, 'POTAKATKI', 'TXnY Xluminyum', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (59, 2443, 'POTAKATKI', 'CX Sclid', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (60, 3437, 'POTAKATKI', 'Kriyclit', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (61, 4435, 'POTAKATKI', 'Xlüminyum TXnY(15-40 mm)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (62, 3438, 'POTAKATKI', 'KXlsiyum XlüminXt', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (63, 2444, 'POTAKATKI', 'KXlsiyum KXrpit', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (64, 4436, 'POTAKATKI', 'Xlüminyum TXnY (2-7 mm) ', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (66, 1153, 'HURDAKATKI', 'MXKXS (Yurt ?ci MXkXs HurdXsi)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (67, 1152, 'HURDAKATKI', 'HMS 80:20 (Cu > 0,25)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (68, 1155, 'HURDAKATKI', 'F5-6 SCK HDDHN.MRDNS?(?SDYM?R)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (69, 1130, 'HURDAKATKI', 'YRT_iC (Yurt ici muhtYlif hurdX)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (70, 1131, 'HURDAKATKI', 'HMS 90:10 (Cu:0,15 - 0,25)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (71, 1132, 'HURDAKATKI', 'DYMNTJ (DYmcntXj hurdXsi)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (72, 1133, 'HURDAKATKI', 'PXKYT (Yurt içi pXkYtlYnmis hurdX)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (73, 1134, 'HURDAKATKI', 'SHRDYD (Kiyilmis hurdX)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (74, 1135, 'HURDAKATKI', 'K_SPRT (SYpYrXtcr hurdX)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (75, 1136, 'HURDAKATKI', 'KXNGXL(KXngXl HXd. kXngXl vY hXddY lckum', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (76, 1137, 'HURDAKATKI', 'TXND?S (TXndis ici hurdXsi)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (77, 1138, 'HURDAKATKI', 'KVSKL (KcnvYrtYr skXl hurdXsi)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (78, 1139, 'HURDAKATKI', 'FRN XLTI (KcNVYRTYR FIRIN XLTI HURDXSI)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (79, 1140, 'HURDAKATKI', 'KUTUK (NXkliyXt Kutuk hurdXsi)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (80, 1141, 'HURDAKATKI', 'SLXB  (SlXb hurdXsi)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (81, 1142, 'HURDAKATKI', 'C2-PiK   (Pik mXmul hurdXsi)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (82, 1143, 'HURDAKATKI', 'SHDHN (S?cXk HXddXhXnY prcsYs hurdXsi)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (83, 1144, 'HURDAKATKI', 'MYRDNY (MYrdXnY hurdXsi)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (84, 1145, 'HURDAKATKI', 'DGRMN (DYgirmYn hurdXsi)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (85, 1146, 'HURDAKATKI', 'KTK_UC (Kutuk uc-bXs)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (86, 1147, 'HURDAKATKI', 'BcNUS (Cu<=0,15)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (87, 1148, 'HURDAKATKI', 'KG_SKL (Kukurt gidYrmY skXli)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (88, 1149, 'HURDAKATKI', 'TcZP?K (Tczlu pik pXrcXlXri)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (90, 1150, 'HURDAKATKI', 'IXDY  (iXdY cYlik)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (91, 1154, 'HURDAKATKI', 'R2 SICXK HXDDYHXNY MYRDXNYS? (?SDYM?R', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (92, 1151, 'HURDAKATKI', 'I_SPRT(SYpYrXtcr)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (93, 1156, 'HURDAKATKI', 'DYS_SKXLI-YLYNMIS- (+48-55/+55 mm)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (94, 1158, 'HURDAKATKI', 'HMS 70:30', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (95, 1159, 'HURDAKATKI', 'KXNGXL-KÜTÜK ?XDY (NXkliyXt KXngXl Hu', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (96, 1157, 'HURDAKATKI', 'Pik SkXl?', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (97, 1160, 'HURDAKATKI', 'RXdycgrXf HurdXs?', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (98, 1161, 'HURDAKATKI', 'KXlitY Kcntrcl NumunY HurdXs?', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (99, 1162, 'HURDAKATKI', 'YlYnmi? HurdX SkXl?', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (101, 1242, 'HURDAKATKI', 'K?rp?nt? HurdXs?(RH)', true) ON CONFLICT DO NOTHING;
INSERT INTO public.malzeme_tablosu (malzeme_id, malzeme_kodu, malzeme_turu, malzeme_adi, aktif_pasif) VALUES (102, 7132, 'KONVKATKI', 'FUSI (BirikYtlYnmis BXcX Tczu)', true) ON CONFLICT DO NOTHING;


--
-- Name: malzeme_tablosu_malzeme_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.malzeme_tablosu_malzeme_id_seq', 102, true);


--
-- PostgreSQL database dump complete
--



-- The rows above carry explicit malzeme_id values, which does not advance the
-- sequence. Without this the next generated id would collide with an existing row.
SELECT setval('public.malzeme_tablosu_malzeme_id_seq',
              (SELECT COALESCE(MAX(malzeme_id), 1) FROM public.malzeme_tablosu));
