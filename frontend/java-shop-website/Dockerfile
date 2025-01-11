FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --frozen-lockfile

COPY . .
RUN npm run build


FROM node:20-alpine AS production

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --production --frozen-lockfile

COPY --from=build /app/dist /app/dist

EXPOSE 4000

CMD ["node", "dist/java-shop-website/server/server.mjs"]

