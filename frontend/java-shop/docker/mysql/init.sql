CREATE DATABASE IF NOT EXISTS 'inventory-service';
GRANT ALL PRIVILEGES ON `inventory-service`.* TO 'admin'@'%';
FLUSH PRIVILEGES;
SELECT host, user FROM mysql.user WHERE user = 'admin';
GRANT ALL PRIVILEGES ON `inventory-service`.* TO 'admin'@'%' IDENTIFIED BY 'admin';
FLUSH PRIVILEGES;
