import mysql from 'mysql2/promise';
import fs from 'fs';
import path from 'path';
import bcrypt from 'bcryptjs';

let pool: mysql.Pool;

// Try to find the db_config.json file
function getDbConfig() {
  const defaultConfig = {
    host: '127.0.0.1',
    port: 3306,
    user: 'root',
    password: 'cj2431365166',
    database: 'anshun_ad_db',
    charset: 'utf8mb4'
  };

  const possiblePaths = [
    path.join(__dirname, '../db_config.json'),
    path.join(__dirname, '../../data-anshun/db_config.json'),
  ];

  for (const configPath of possiblePaths) {
    if (fs.existsSync(configPath)) {
      try {
        console.log(`Loading database configuration from ${configPath}`);
        const parsed = JSON.parse(fs.readFileSync(configPath, 'utf-8'));
        return { ...defaultConfig, ...parsed };
      } catch (err) {
        console.error(`Failed to parse database configuration at ${configPath}:`, err);
      }
    }
  }

  console.log('Using default database configuration (localhost:3306)');
  return defaultConfig;
}

export function initDb() {
  if (pool) return pool;

  const config = getDbConfig();
  pool = mysql.createPool({
    host: config.host,
    port: Number(config.port),
    user: config.user,
    password: config.password,
    database: config.database,
    charset: config.charset,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
  });

  return pool;
}

export async function query<T = any>(sql: string, params?: any[]): Promise<T[]> {
  const p = initDb();
  const [rows] = await p.query(sql, params);
  return rows as T[];
}

export async function ensureUserTable() {
  const ddl = `
    CREATE TABLE IF NOT EXISTS \`t_user\` (
      \`user_id\` INT AUTO_INCREMENT COMMENT '用户ID',
      \`username\` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
      \`password_hash\` VARCHAR(255) NOT NULL COMMENT '密码哈希',
      \`nickname\` VARCHAR(50) COMMENT '昵称',
      \`role\` VARCHAR(20) DEFAULT 'user' COMMENT '角色(admin/user)',
      \`status\` VARCHAR(20) DEFAULT 'active' COMMENT '状态(active/disabled)',
      \`create_time\` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      PRIMARY KEY (\`user_id\`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';
  `;

  try {
    await query(ddl);
    console.log('Verified user table (t_user) exists.');

    // Check if admin user exists
    const users = await query('SELECT * FROM t_user WHERE username = ?', ['admin']);
    if (users.length === 0) {
      console.log('No default admin found. Creating user "admin" with password "admin123"...');
      const hash = await bcrypt.hash('admin123', 10);
      await query(
        'INSERT INTO t_user (username, password_hash, nickname, role, status) VALUES (?, ?, ?, ?, ?)',
        ['admin', hash, '系统管理员', 'admin', 'active']
      );
      console.log('Default administrator created successfully.');
    }
  } catch (error) {
    console.error('Error during database initialization (t_user check):', error);
  }
}
