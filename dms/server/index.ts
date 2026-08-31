import express, { Response } from 'express';
import cors from 'cors';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import path from 'path';
import fs from 'fs';
import { query, ensureUserTable } from './db';
import { authMiddleware, JWT_SECRET, AuthRequest } from './auth';

const app = express();
const PORT = Number(process.env.PORT || 3000);

app.use(cors());
app.use(express.json());

// 1. AUTHENTICATION API
app.post('/api/auth/register', async (req, res) => {
  const { username, password, nickname } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: '用户名和密码不能为空' });
  }

  try {
    const existing = await query('SELECT * FROM t_user WHERE username = ?', [username]);
    if (existing.length > 0) {
      return res.status(400).json({ error: '用户名已存在' });
    }

    const hash = await bcrypt.hash(password, 10);
    await query(
      'INSERT INTO t_user (username, password_hash, nickname, role, status) VALUES (?, ?, ?, ?, ?)',
      [username, hash, nickname || username, 'user', 'active']
    );

    res.json({ message: '注册成功' });
  } catch (err: any) {
    res.status(500).json({ error: err.message || '注册失败，服务器内部错误' });
  }
});

app.post('/api/auth/login', async (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: '请输入用户名和密码' });
  }

  try {
    const users = await query('SELECT * FROM t_user WHERE username = ?', [username]);
    if (users.length === 0) {
      return res.status(400).json({ error: '用户名或密码错误' });
    }

    const user = users[0];
    if (user.status === 'disabled') {
      return res.status(400).json({ error: '该账户已被禁用' });
    }

    const isValid = await bcrypt.compare(password, user.password_hash);
    if (!isValid) {
      return res.status(400).json({ error: '用户名或密码错误' });
    }

    const token = jwt.sign(
      { userId: user.user_id, username: user.username, role: user.role },
      JWT_SECRET,
      { expiresIn: '24h' }
    );

    res.json({
      token,
      user: {
        userId: user.user_id,
        username: user.username,
        nickname: user.nickname || user.username,
        role: user.role
      }
    });
  } catch (err: any) {
    res.status(500).json({ error: err.message || '登录失败' });
  }
});

app.get('/api/auth/me', authMiddleware, async (req: AuthRequest, res) => {
  if (!req.user) return res.status(401).json({ error: '未登录' });

  try {
    const users = await query(
      'SELECT user_id, username, nickname, role, status, create_time FROM t_user WHERE user_id = ?',
      [req.user.userId]
    );
    if (users.length === 0) {
      return res.status(404).json({ error: '用户不存在' });
    }
    res.json(users[0]);
  } catch (err: any) {
    res.status(500).json({ error: err.message || '获取用户信息失败' });
  }
});

// 2. ADVERTISING POSITION API
app.get('/api/positions', authMiddleware, async (req, res) => {
  const page = Number(req.query.page || 1);
  const limit = Number(req.query.limit || 10);
  const offset = (page - 1) * limit;

  const district = req.query.district as string;
  const status = req.query.status as string;
  const search = req.query.search as string;

  let countSql = 'SELECT COUNT(*) as count FROM t_ad_position WHERE 1=1';
  let selectSql = 'SELECT * FROM t_ad_position WHERE 1=1';
  const params: any[] = [];

  if (district) {
    countSql += ' AND district = ?';
    selectSql += ' AND district = ?';
    params.push(district);
  }
  if (status) {
    countSql += ' AND status = ?';
    selectSql += ' AND status = ?';
    params.push(status);
  }
  if (search) {
    countSql += ' AND (ad_location LIKE ? OR road_name LIKE ? OR ad_position_code LIKE ?)';
    selectSql += ' AND (ad_location LIKE ? OR road_name LIKE ? OR ad_position_code LIKE ?)';
    const likeParam = `%${search}%`;
    params.push(likeParam, likeParam, likeParam);
  }

  selectSql += ' ORDER BY ad_position_id ASC LIMIT ? OFFSET ?';
  const selectParams = [...params, limit, offset];

  try {
    const countResult = await query(countSql, params);
    const total = countResult[0].count;

    const data = await query(selectSql, selectParams);

    // Calculate real-time status dynamically based on current date for each position
    // If there is an active lease, set status = 'leased', else keep DB status or 'vacant'
    const today = new Date().toISOString().split('T')[0];
    for (const pos of data) {
      const activeLeases = await query(
        'SELECT COUNT(*) as cnt FROM t_ad_lease_detail WHERE ad_position_code = ? AND lease_start_date <= ? AND lease_end_date >= ?',
        [pos.ad_position_code, today, today]
      );
      pos.status = activeLeases[0].cnt > 0 ? 'leased' : 'vacant';
    }

    res.json({ total, data });
  } catch (err: any) {
    res.status(500).json({ error: err.message || '获取广告点位列表失败' });
  }
});

app.get('/api/positions/:code', authMiddleware, async (req, res) => {
  const code = req.params.code;

  try {
    const positions = await query('SELECT * FROM t_ad_position WHERE ad_position_code = ?', [code]);
    if (positions.length === 0) {
      return res.status(404).json({ error: '点位未找到' });
    }
    const position = positions[0];

    // Get valuation
    const valuations = await query('SELECT * FROM t_ad_position_valuation WHERE ad_position_code = ?', [code]);
    position.valuation = valuations[0] || null;

    // Get lease history (sorted newest to oldest)
    const leases = await query(
      'SELECT * FROM t_ad_lease_detail WHERE ad_position_code = ? ORDER BY lease_start_date DESC',
      [code]
    );
    position.leaseHistory = leases;

    res.json(position);
  } catch (err: any) {
    res.status(500).json({ error: err.message || '获取广告点位详情失败' });
  }
});

app.post('/api/positions', authMiddleware, async (req, res) => {
  const {
    ad_position_code,
    ad_location,
    single_side_area,
    total_ad_area,
    ad_specification,
    longitude,
    latitude,
    district,
    road_name,
    status,
    remark
  } = req.body;

  if (!ad_position_code || !ad_location || !single_side_area || !total_ad_area || !ad_specification) {
    return res.status(400).json({ error: '点位编码、设立位置、规格面积等核心参数必填' });
  }

  try {
    const existing = await query('SELECT * FROM t_ad_position WHERE ad_position_code = ?', [ad_position_code]);
    if (existing.length > 0) {
      return res.status(400).json({ error: '该点位编码已存在，请勿重复创建' });
    }

    await query(
      `INSERT INTO t_ad_position 
       (ad_position_code, ad_location, single_side_area, total_ad_area, ad_specification, longitude, latitude, district, road_name, status, remark) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        ad_position_code,
        ad_location,
        single_side_area,
        Number(total_ad_area),
        ad_specification,
        longitude ? Number(longitude) : null,
        latitude ? Number(latitude) : null,
        district || null,
        road_name || null,
        status || 'vacant',
        remark || null
      ]
    );

    res.json({ message: '创建点位成功', code: ad_position_code });
  } catch (err: any) {
    res.status(500).json({ error: err.message || '新建点位失败' });
  }
});

app.put('/api/positions/:code', authMiddleware, async (req, res) => {
  const code = req.params.code;
  const {
    ad_location,
    single_side_area,
    total_ad_area,
    ad_specification,
    longitude,
    latitude,
    district,
    road_name,
    status,
    remark
  } = req.body;

  if (!ad_location || !single_side_area || !total_ad_area || !ad_specification) {
    return res.status(400).json({ error: '点位设立位置、规格面积等核心参数必填' });
  }

  try {
    const existing = await query('SELECT * FROM t_ad_position WHERE ad_position_code = ?', [code]);
    if (existing.length === 0) {
      return res.status(404).json({ error: '未找到待更新的点位' });
    }

    await query(
      `UPDATE t_ad_position SET 
       ad_location = ?, 
       single_side_area = ?, 
       total_ad_area = ?, 
       ad_specification = ?, 
       longitude = ?, 
       latitude = ?, 
       district = ?, 
       road_name = ?, 
       status = ?, 
       remark = ? 
       WHERE ad_position_code = ?`,
      [
        ad_location,
        single_side_area,
        Number(total_ad_area),
        ad_specification,
        longitude ? Number(longitude) : null,
        latitude ? Number(latitude) : null,
        district || null,
        road_name || null,
        status || 'vacant',
        remark || null,
        code
      ]
    );

    // Also update cascaded location information in historical details for consistency
    await query(
      `UPDATE t_ad_lease_detail SET 
       ad_location = ?, 
       single_side_area = ?, 
       total_ad_area = ?, 
       ad_specification = ? 
       WHERE ad_position_code = ?`,
      [ad_location, single_side_area, Number(total_ad_area), ad_specification, code]
    );

    res.json({ message: '更新点位成功', code });
  } catch (err: any) {
    res.status(500).json({ error: err.message || '更新点位失败' });
  }
});

app.delete('/api/positions/:code', authMiddleware, async (req, res) => {
  const code = req.params.code;

  try {
    const existing = await query('SELECT * FROM t_ad_position WHERE ad_position_code = ?', [code]);
    if (existing.length === 0) {
      return res.status(404).json({ error: '未找到待删除的点位' });
    }

    await query('DELETE FROM t_ad_position WHERE ad_position_code = ?', [code]);
    res.json({ message: '删除点位成功', code });
  } catch (err: any) {
    res.status(500).json({ error: err.message || '删除点位失败' });
  }
});

// 3. LEASE CONTRACT API
app.get('/api/leases', authMiddleware, async (req, res) => {
  const page = Number(req.query.page || 1);
  const limit = Number(req.query.limit || 10);
  const offset = (page - 1) * limit;

  const search = req.query.search as string;
  const lessee = req.query.lessee_company as string;

  let countSql = 'SELECT COUNT(*) as count FROM t_ad_lease_detail WHERE 1=1';
  let selectSql = 'SELECT * FROM t_ad_lease_detail WHERE 1=1';
  const params: any[] = [];

  if (search) {
    countSql += ' AND (contract_code LIKE ? OR ad_location LIKE ? OR ad_position_code LIKE ?)';
    selectSql += ' AND (contract_code LIKE ? OR ad_location LIKE ? OR ad_position_code LIKE ?)';
    const likeParam = `%${search}%`;
    params.push(likeParam, likeParam, likeParam);
  }
  if (lessee) {
    countSql += ' AND lessee_company LIKE ?';
    selectSql += ' AND lessee_company LIKE ?';
    params.push(`%${lessee}%`);
  }

  selectSql += ' ORDER BY lease_start_date DESC LIMIT ? OFFSET ?';
  const selectParams = [...params, limit, offset];

  try {
    const countResult = await query(countSql, params);
    const total = countResult[0].count;

    const data = await query(selectSql, selectParams);
    res.json({ total, data });
  } catch (err: any) {
    res.status(500).json({ error: err.message || '获取承租明细列表失败' });
  }
});

app.post('/api/leases', authMiddleware, async (req, res) => {
  const {
    contract_code,
    ad_position_code,
    lessee_code,
    lessee_company,
    lease_rent,
    lease_start_date,
    lease_end_date,
    contract_sign_date
  } = req.body;

  if (!contract_code || !ad_position_code || !lessee_code || !lessee_company || lease_rent === undefined || !lease_start_date || !lease_end_date || !contract_sign_date) {
    return res.status(400).json({ error: '合同编码、点位编码、承租方、租金及起止日期均为必填项' });
  }

  try {
    // Resolve position details
    const positions = await query('SELECT * FROM t_ad_position WHERE ad_position_code = ?', [ad_position_code]);
    if (positions.length === 0) {
      return res.status(400).json({ error: `指定的广告点位编码 "${ad_position_code}" 不存在` });
    }
    const pos = positions[0];

    // Calculate term days
    const start = new Date(lease_start_date);
    const end = new Date(lease_end_date);
    const lease_term = Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
    if (lease_term <= 0) {
      return res.status(400).json({ error: '租期结束日期必须在开始日期之后' });
    }

    await query(
      `INSERT INTO t_ad_lease_detail 
       (contract_code, ad_position_code, ad_location, single_side_area, total_ad_area, ad_specification, lessee_code, lessee_company, lease_rent, lease_term, lease_start_date, lease_end_date, contract_sign_date) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        contract_code,
        ad_position_code,
        pos.ad_location,
        pos.single_side_area,
        pos.total_ad_area,
        pos.ad_specification,
        lessee_code,
        lessee_company,
        Number(lease_rent),
        lease_term,
        lease_start_date,
        lease_end_date,
        contract_sign_date
      ]
    );

    res.json({ message: '录入合同成功' });
  } catch (err: any) {
    res.status(500).json({ error: err.message || '录入合同失败' });
  }
});

app.put('/api/leases/:id', authMiddleware, async (req, res) => {
  const id = Number(req.params.id);
  const {
    contract_code,
    ad_position_code,
    lessee_code,
    lessee_company,
    lease_rent,
    lease_start_date,
    lease_end_date,
    contract_sign_date
  } = req.body;

  if (!contract_code || !ad_position_code || !lessee_code || !lessee_company || lease_rent === undefined || !lease_start_date || !lease_end_date || !contract_sign_date) {
    return res.status(400).json({ error: '合同编码、点位编码、承租方、租金及起止日期均为必填项' });
  }

  try {
    const existing = await query('SELECT * FROM t_ad_lease_detail WHERE ad_lease_id = ?', [id]);
    if (existing.length === 0) {
      return res.status(404).json({ error: '未找到该待修改的合同记录' });
    }

    // Resolve position details in case position code was changed
    const positions = await query('SELECT * FROM t_ad_position WHERE ad_position_code = ?', [ad_position_code]);
    if (positions.length === 0) {
      return res.status(400).json({ error: `指定的广告点位编码 "${ad_position_code}" 不存在` });
    }
    const pos = positions[0];

    // Calculate term days
    const start = new Date(lease_start_date);
    const end = new Date(lease_end_date);
    const lease_term = Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
    if (lease_term <= 0) {
      return res.status(400).json({ error: '租期结束日期必须在开始日期之后' });
    }

    await query(
      `UPDATE t_ad_lease_detail SET 
       contract_code = ?, 
       ad_position_code = ?, 
       ad_location = ?, 
       single_side_area = ?, 
       total_ad_area = ?, 
       ad_specification = ?, 
       lessee_code = ?, 
       lessee_company = ?, 
       lease_rent = ?, 
       lease_term = ?, 
       lease_start_date = ?, 
       lease_end_date = ?, 
       contract_sign_date = ? 
       WHERE ad_lease_id = ?`,
      [
        contract_code,
        ad_position_code,
        pos.ad_location,
        pos.single_side_area,
        pos.total_ad_area,
        pos.ad_specification,
        lessee_code,
        lessee_company,
        Number(lease_rent),
        lease_term,
        lease_start_date,
        lease_end_date,
        contract_sign_date,
        id
      ]
    );

    res.json({ message: '修改合同成功' });
  } catch (err: any) {
    res.status(500).json({ error: err.message || '更新合同失败' });
  }
});

app.delete('/api/leases/:id', authMiddleware, async (req, res) => {
  const id = Number(req.params.id);

  try {
    const existing = await query('SELECT * FROM t_ad_lease_detail WHERE ad_lease_id = ?', [id]);
    if (existing.length === 0) {
      return res.status(404).json({ error: '未找到待删除的合同记录' });
    }

    await query('DELETE FROM t_ad_lease_detail WHERE ad_lease_id = ?', [id]);
    res.json({ message: '删除合同成功', id });
  } catch (err: any) {
    res.status(500).json({ error: err.message || '删除合同失败' });
  }
});

// 4. STATS & VISUALIZATION API
app.get('/api/stats/overview', authMiddleware, async (req, res) => {
  try {
    // Total Positions
    const posCountRes = await query('SELECT COUNT(*) as count FROM t_ad_position');
    const totalPositions = posCountRes[0].count;

    // Leased Positions (currently active)
    const today = new Date().toISOString().split('T')[0];
    const leasedCountRes = await query(
      'SELECT COUNT(DISTINCT ad_position_code) as count FROM t_ad_lease_detail WHERE lease_start_date <= ? AND lease_end_date >= ?',
      [today, today]
    );
    const leasedPositions = leasedCountRes[0].count;
    const leasedRate = totalPositions > 0 ? Number(((leasedPositions / totalPositions) * 100).toFixed(2)) : 0;

    // Total Revenue (accumulated rent in millions/thousands)
    const revSumRes = await query('SELECT SUM(lease_rent) as sum FROM t_ad_lease_detail');
    const totalRevenue = Number(Number(revSumRes[0].sum || 0).toFixed(2));

    // Arrears Amount (from t_ad_revenue_stat)
    const arrearsRes = await query(
      "SELECT SUM(period_rent) as sum FROM t_ad_revenue_stat WHERE rent_status IN ('欠费', '待收回')"
    );
    const arrearsAmount = Number(Number(arrearsRes[0].sum || 0).toFixed(2));

    res.json({
      totalPositions,
      leasedPositions,
      leasedRate,
      totalRevenue,
      arrearsAmount
    });
  } catch (err: any) {
    res.status(500).json({ error: err.message || '获取指标概览数据失败' });
  }
});

app.get('/api/stats/revenue-trend', authMiddleware, async (req, res) => {
  const year = req.query.year ? Number(req.query.year) : null;

  try {
    if (year) {
      // Monthly trend for specific year
      const sql = `
        SELECT stat_month as month, SUM(period_rent) as revenue 
        FROM t_ad_revenue_stat 
        WHERE stat_cycle = 'month' AND stat_year = ? 
        GROUP BY stat_month 
        ORDER BY stat_month ASC
      `;
      const data = await query(sql, [year]);
      res.json(data);
    } else {
      // Yearly trend
      const sql = `
        SELECT stat_year as year, SUM(period_rent) as revenue 
        FROM t_ad_revenue_stat 
        WHERE stat_cycle = 'year' 
        GROUP BY stat_year 
        ORDER BY stat_year ASC
      `;
      const data = await query(sql);
      res.json(data);
    }
  } catch (err: any) {
    res.status(500).json({ error: err.message || '获取收入趋势失败' });
  }
});

app.get('/api/stats/industry-distribution', authMiddleware, async (req, res) => {
  try {
    // Return latest year (2025) stats by default or sum contributions
    const sql = `
      SELECT industry_name, SUM(total_rent_contribution) as total_rent, SUM(total_ad_position) as total_pos 
      FROM t_lease_enterprise_industry_dist 
      WHERE statistic_dimension = 'year' AND stat_interval_start = '2025-01-01' 
      GROUP BY industry_name 
      ORDER BY total_rent DESC
    `;
    const data = await query(sql);
    res.json(data);
  } catch (err: any) {
    res.status(500).json({ error: err.message || '获取行业分布数据失败' });
  }
});

app.get('/api/stats/hot-areas', authMiddleware, async (req, res) => {
  try {
    // Get average stats per hot area from the analysis table
    const sql = `
      SELECT area_name, 
             ROUND(AVG(ad_position_rent_rate), 2) as rent_rate, 
             ROUND(AVG(avg_rent_per_sqm), 2) as avg_rent, 
             ROUND(AVG(ad_turnover_rate), 2) as turnover_rate 
      FROM t_ad_hot_area_analysis 
      GROUP BY area_name 
      ORDER BY rent_rate DESC
    `;
    const data = await query(sql);
    res.json(data);
  } catch (err: any) {
    res.status(500).json({ error: err.message || '获取热门区域分析数据失败' });
  }
});

app.get('/api/stats/map-positions', authMiddleware, async (req, res) => {
  try {
    const data = await query(
      'SELECT ad_position_code, ad_location, longitude, latitude, status, total_ad_area, ad_specification, district, road_name FROM t_ad_position'
    );
    // Dynamic status calculation
    const today = new Date().toISOString().split('T')[0];
    for (const pos of data) {
      const activeLeases = await query(
        'SELECT COUNT(*) as cnt FROM t_ad_lease_detail WHERE ad_position_code = ? AND lease_start_date <= ? AND lease_end_date >= ?',
        [pos.ad_position_code, today, today]
      );
      pos.status = activeLeases[0].cnt > 0 ? 'leased' : 'vacant';
    }
    res.json(data);
  } catch (err: any) {
    res.status(500).json({ error: err.message || '获取地图广告位点位失败' });
  }
});

app.get('/api/stats/top-enterprises', authMiddleware, async (req, res) => {
  try {
    // Get top 10 enterprises by total spend in 2025
    const sql = `
      SELECT lessee_company, SUM(total_rent_input) as total_rent 
      FROM t_lease_enterprise_ad_fund_stat 
      WHERE statistic_dimension = 'year' AND stat_interval_start = '2025-01-01' 
      GROUP BY lessee_company 
      ORDER BY total_rent DESC 
      LIMIT 10
    `;
    const data = await query(sql);
    res.json(data);
  } catch (err: any) {
    res.status(500).json({ error: err.message || '获取企业投入排行失败' });
  }
});

// 5. STATIC FILES HOSTING (Vue Frontend SPA integration)
const distPath = path.join(__dirname, '../dist');
if (fs.existsSync(distPath)) {
  console.log(`Serving static files from frontend build: ${distPath}`);
  app.use(express.static(distPath));
  app.get('*', (req, res) => {
    res.sendFile(path.join(distPath, 'index.html'));
  });
}

// 6. DB INITIATION & START SERVER
const serverInit = async () => {
  console.log('Starting DMS server initialization...');
  await ensureUserTable();
  app.listen(PORT, () => {
    console.log(`===================================================`);
    console.log(` DMS Server is running on port ${PORT}`);
    console.log(` API base url: http://localhost:${PORT}/api`);
    console.log(`===================================================`);
  });
};

serverInit().catch(err => {
  console.error('Fatal: Failed to start DMS Server:', err);
  process.exit(1);
});
