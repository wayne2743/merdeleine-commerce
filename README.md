## 跑某一個微服務
```
./gradlew :services:order-service:bootRun
```

## 打包某一個服務
```
 ./gradlew :services:order-service:bootJar
```

## CI 用（標準）
```
 ./gradlew build
```


## DB
```
  POSTGRES_USER: merdeleine
  POSTGRES_PASSWORD: merdeleine_pw
  POSTGRES_DB: postgres
```


## Email
### spring-boot-test
```
  for testing:
  
  EMAIL_HOST: smtp.gmail.com
  EMAIL_PORT: 587
  EMAIL_USERNAME: wayne2347@gmail.com
  EMAIL_PASSWORD: iumihqrmpvsglwls
```
