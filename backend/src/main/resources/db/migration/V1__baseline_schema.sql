CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `authorities` json DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `username` varchar(255) NOT NULL,
  `uuid` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK6km2m9i3vjuy36rnvkgj1l61s` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `wallet` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `currency_code` enum('EUR','HUF','USD') DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '0',
  `type` enum('DEFAULT','SAVINGS') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgbusavqq0bdaodex4ee6v0811` (`user_id`),
  CONSTRAINT `FKgbusavqq0bdaodex4ee6v0811` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `wallet_chk_1` CHECK ((`status` between 0 and 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '0',
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `lang` enum('DE','EN','HU') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7ffrpnxaflomhdh0qfk2jcndo` (`user_id`),
  CONSTRAINT `FK7ffrpnxaflomhdh0qfk2jcndo` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `category_chk_1` CHECK ((`status` between 0 and 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `price_sum` decimal(10,2) NOT NULL,
  `status` int NOT NULL DEFAULT '0',
  `transaction_date` date NOT NULL,
  `transaction_type` enum('INCOME','OUTCOME') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `wallet_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqwv7rmvc8va8rep7piikrojds` (`user_id`),
  KEY `FK1y5uaof4j1b7fo1ldgkunxq9t` (`wallet_id`),
  CONSTRAINT `FK1y5uaof4j1b7fo1ldgkunxq9t` FOREIGN KEY (`wallet_id`) REFERENCES `wallet` (`id`),
  CONSTRAINT `FKqwv7rmvc8va8rep7piikrojds` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `transaction_details` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `price` decimal(10,2) DEFAULT NULL,
  `unit_price` decimal(10,2) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `weight` decimal(10,2) DEFAULT NULL,
  `transaction_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKm5vjjt9jqvf7y69innpgqnipr` (`transaction_id`),
  CONSTRAINT `FKm5vjjt9jqvf7y69innpgqnipr` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `transaction_detail_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint DEFAULT NULL,
  `transaction_detail_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKj3sg75qxr1pnhneg8628mbobl` (`category_id`),
  KEY `FK3n9vnj5429o8huhe5kd47rmkr` (`transaction_detail_id`),
  CONSTRAINT `FK3n9vnj5429o8huhe5kd47rmkr` FOREIGN KEY (`transaction_detail_id`) REFERENCES `transaction_details` (`id`),
  CONSTRAINT `FKj3sg75qxr1pnhneg8628mbobl` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
