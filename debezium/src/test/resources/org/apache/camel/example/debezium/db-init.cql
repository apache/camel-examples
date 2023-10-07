CREATE KEYSPACE dbzSink WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};

USE dbzSink;

CREATE TABLE products (
  id int PRIMARY KEY,
  name varchar,
  description varchar,
  weight float
);