# Build asamasi. Once sadece package dosyalari kopyalanir: kaynak kod degistiginde
# npm ci katmani cache'ten gelir, bagimliliklar yeniden indirilmez.
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Calisma asamasi. Node burada yok; geriye sadece dist/ icindeki statik dosyalar
# ve onlari servis eden nginx kaliyor (imaj ~50 MB).
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
