docker run -d \
  --name postgres \
  -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=my_app \
  -v /Users/liushiyou/pgdata:/var/lib/postgresql/data \
  pgvector/pgvector:pg16
