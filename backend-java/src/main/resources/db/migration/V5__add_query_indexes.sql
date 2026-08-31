CREATE INDEX idx_position_status_district ON t_ad_position (status, district);
CREATE INDEX idx_lease_contract_code ON t_ad_lease_detail (contract_code);
CREATE INDEX idx_lease_period ON t_ad_lease_detail (lease_start_date, lease_end_date);
