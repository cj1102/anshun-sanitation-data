ALTER TABLE t_ad_position
  ADD CONSTRAINT chk_position_total_area CHECK (total_ad_area > 0),
  ADD CONSTRAINT chk_position_status CHECK (status IN ('vacant', 'leased')),
  ADD CONSTRAINT chk_position_deleted CHECK (deleted IN (0, 1));

ALTER TABLE t_ad_lease_detail
  ADD CONSTRAINT chk_lease_total_area CHECK (total_ad_area > 0),
  ADD CONSTRAINT chk_lease_rent CHECK (lease_rent >= 0),
  ADD CONSTRAINT chk_lease_term CHECK (lease_term > 0),
  ADD CONSTRAINT chk_lease_period CHECK (lease_end_date >= lease_start_date),
  ADD CONSTRAINT chk_lease_approval_status CHECK (approval_status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED')),
  ADD CONSTRAINT chk_lease_deleted CHECK (deleted IN (0, 1));
