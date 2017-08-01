# anet-mssql-linux

Docker image for running a mssql database for [anet](https://github.com/nci-agency/anet)

# usage

* run the image:
  ```sh
  docker run -d \
      -e "ACCEPT_EULA=Y" \
      -e "SA_PASSWORD=password" \
      -e "DB_NAME=dbName" \
      -e "DB_USER=dbUserName" \
      -e "DB_USER_PASSWORD=password" \ 
      -p 1433:1433 \
      ncia/anet-mssql-linux
  ```

This will start a mssql server 14.0.600.250-2 on ubuntu:16.04 with full text search enabled, and it will create an empty db (dbName) with a sysadmin (DB_USER/DB_USER_PASSWORD)