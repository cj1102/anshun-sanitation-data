export interface User {
  user_id?: number;
  username: string;
  password_hash: string;
  nickname?: string;
  role?: string;
  status?: string;
  create_time?: Date;
}

export interface AdPosition {
  ad_position_id: number;
  ad_position_code: string;
  ad_location: string;
  single_side_area: string;
  total_ad_area: number;
  ad_specification: string;
  longitude: number;
  latitude: number;
  district: string;
  road_name: string;
  status: 'vacant' | 'leased' | 'maintenance';
  remark: string | null;
  create_time: Date;
}

export interface AdPositionValuation {
  valuation_id: number;
  ad_position_code: string;
  ad_location: string;
  single_side_area: string;
  total_ad_area: number;
  ad_specification: string;
  ad_count: number;
  discounted_rent: number;
  total_assessed_value: number;
  valuation_date: string | Date;
  lessee_company: string;
  valuation_method: string;
  create_time: Date;
}

export interface AdLeaseDetail {
  ad_lease_id: number;
  contract_code: string;
  ad_position_code: string;
  ad_location: string;
  single_side_area: string;
  total_ad_area: number;
  ad_specification: string;
  lessee_code: string;
  lessee_company: string;
  lease_rent: number;
  lease_term: number;
  lease_start_date: string | Date;
  lease_end_date: string | Date;
  contract_sign_date: string | Date;
  create_time: Date;
}

export interface AdRevenueStat {
  revenue_stat_id: number;
  ad_position_code: string;
  ad_location: string;
  stat_cycle: 'year' | 'month';
  stat_year: number;
  stat_month: number | null;
  period_rent: number;
  rent_status: string;
  create_time: Date;
}

export interface LeaseEnterpriseAdFundStat {
  stat_id: number;
  lessee_code: string;
  lessee_company: string;
  statistic_dimension: 'year' | 'month';
  stat_interval_start: string | Date;
  stat_interval_end: string | Date;
  ad_position_count: number;
  total_rent_input: number;
  create_time: Date;
}

export interface LeaseEnterpriseIndustryDist {
  industry_dist_id: number;
  statistic_dimension: 'year' | 'month';
  stat_interval_start: string | Date;
  stat_interval_end: string | Date;
  industry_code: string;
  industry_name: string;
  lessee_enterprise_num: number;
  total_ad_position: number;
  avg_position_per_enterprise: number;
  total_rent_contribution: number;
  rent_accounting_ratio: number;
  create_time: Date;
}

export interface AdHotAreaAnalysis {
  hot_area_ana_id: number;
  area_name: string;
  total_ad_position: number;
  rented_ad_position: number;
  ad_position_rent_rate: number;
  avg_rent_per_sqm: number;
  avg_lease_term: number;
  ad_turnover_rate: number;
  main_audience_type: string;
  stat_interval_start: string | Date;
  stat_interval_end: string | Date;
  create_time: Date;
}
