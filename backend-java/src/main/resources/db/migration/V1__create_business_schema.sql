CREATE TABLE IF NOT EXISTS t_ad_position (
  ad_position_id INT AUTO_INCREMENT PRIMARY KEY, ad_position_code VARCHAR(32) NOT NULL UNIQUE,
  ad_location VARCHAR(255) NOT NULL, single_side_area VARCHAR(50) NOT NULL, total_ad_area INT NOT NULL,
  ad_specification VARCHAR(50) NOT NULL, longitude DECIMAL(10,6), latitude DECIMAL(10,6),
  district VARCHAR(50), road_name VARCHAR(100), status VARCHAR(20) DEFAULT 'vacant', remark TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP, INDEX idx_position_district (district), INDEX idx_position_road (road_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_ad_position_valuation (
  valuation_id INT AUTO_INCREMENT PRIMARY KEY, ad_position_code VARCHAR(32) NOT NULL, ad_location VARCHAR(255) NOT NULL,
  single_side_area VARCHAR(50) NOT NULL, total_ad_area INT NOT NULL, ad_specification VARCHAR(50) NOT NULL,
  ad_count INT DEFAULT 1, discounted_rent DECIMAL(10,2) NOT NULL, total_assessed_value DECIMAL(10,2) NOT NULL,
  valuation_date DATE NOT NULL, lessee_company VARCHAR(100) NOT NULL, valuation_method VARCHAR(50) DEFAULT '收益折现法',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_valuation_position FOREIGN KEY (ad_position_code) REFERENCES t_ad_position(ad_position_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_ad_lease_detail (
  ad_lease_id INT AUTO_INCREMENT PRIMARY KEY, contract_code VARCHAR(50) NOT NULL, ad_position_code VARCHAR(32) NOT NULL,
  ad_location VARCHAR(255) NOT NULL, single_side_area VARCHAR(50) NOT NULL, total_ad_area INT NOT NULL,
  ad_specification VARCHAR(50) NOT NULL, lessee_code VARCHAR(32) NOT NULL, lessee_company VARCHAR(100) NOT NULL,
  lease_rent DECIMAL(18,2) NOT NULL, lease_term INT NOT NULL, lease_start_date DATE NOT NULL, lease_end_date DATE NOT NULL,
  contract_sign_date DATE NOT NULL, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_lease_position (ad_position_code), INDEX idx_lease_lessee (lessee_code),
  CONSTRAINT fk_lease_position FOREIGN KEY (ad_position_code) REFERENCES t_ad_position(ad_position_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_ad_revenue_stat (
  revenue_stat_id INT AUTO_INCREMENT PRIMARY KEY, ad_position_code VARCHAR(32) NOT NULL, ad_location VARCHAR(255) NOT NULL,
  stat_cycle VARCHAR(10) NOT NULL, stat_year INT NOT NULL, stat_month INT, period_rent DECIMAL(18,2) NOT NULL,
  rent_status VARCHAR(20) NOT NULL, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_revenue_position (ad_position_code), INDEX idx_revenue_period (stat_year, stat_month),
  CONSTRAINT fk_revenue_position FOREIGN KEY (ad_position_code) REFERENCES t_ad_position(ad_position_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_lease_enterprise_ad_fund_stat (
  stat_id INT AUTO_INCREMENT PRIMARY KEY, lessee_code VARCHAR(32) NOT NULL, lessee_company VARCHAR(100) NOT NULL,
  statistic_dimension VARCHAR(10) NOT NULL, stat_interval_start DATE NOT NULL, stat_interval_end DATE NOT NULL,
  ad_position_count INT NOT NULL, total_rent_input DECIMAL(18,2) NOT NULL, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_fund_lessee (lessee_code), INDEX idx_fund_interval (stat_interval_start, stat_interval_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_lease_enterprise_industry_dist (
  industry_dist_id INT AUTO_INCREMENT PRIMARY KEY, statistic_dimension VARCHAR(10) NOT NULL,
  stat_interval_start DATE NOT NULL, stat_interval_end DATE NOT NULL, industry_code VARCHAR(20) NOT NULL,
  industry_name VARCHAR(50) NOT NULL, lessee_enterprise_num INT NOT NULL, total_ad_position INT NOT NULL,
  avg_position_per_enterprise DECIMAL(10,2) NOT NULL, total_rent_contribution DECIMAL(18,2) NOT NULL,
  rent_accounting_ratio DECIMAL(5,2) NOT NULL, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_industry_interval (stat_interval_start, stat_interval_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_ad_hot_area_analysis (
  hot_area_ana_id INT AUTO_INCREMENT PRIMARY KEY, area_name VARCHAR(50) NOT NULL, total_ad_position INT NOT NULL,
  rented_ad_position INT NOT NULL, ad_position_rent_rate DECIMAL(5,2) NOT NULL, avg_rent_per_sqm DECIMAL(10,2) NOT NULL,
  avg_lease_term INT NOT NULL, ad_turnover_rate DECIMAL(5,2) NOT NULL, main_audience_type VARCHAR(100),
  stat_interval_start DATE NOT NULL, stat_interval_end DATE NOT NULL, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_hot_interval (stat_interval_start, stat_interval_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
