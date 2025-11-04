-- Script to reset database for FashionFlex
-- Run this in MySQL Workbench or MySQL command line

DROP DATABASE IF EXISTS ecommerce_db;
CREATE DATABASE ecommerce_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the database
USE ecommerce_db;

-- The tables will be created automatically by Hibernate on application startup
