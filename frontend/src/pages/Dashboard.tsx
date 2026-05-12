import React, { useEffect, useState } from 'react';
import { Card, Col, Row, Spin, Typography, Progress, Tag, Divider } from 'antd';
import {
  ShopOutlined,
  TableOutlined,
  GlobalOutlined,
  CheckCircleOutlined,
  RiseOutlined,
  AimOutlined,
} from '@ant-design/icons';
import { ratesheetApi } from '../api/ratesheetApi';
import type { DashboardStats } from '../api/ratesheetApi';

const { Title, Text } = Typography;

const StatCard: React.FC<{
  title: string;
  value: number;
  icon: React.ReactNode;
  color: string;
  bg: string;
  suffix?: string;
}> = ({ title, value, icon, color, bg, suffix }) => (
  <Card
    style={{ borderRadius: 12, border: 'none', boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}
    bodyStyle={{ padding: '20px 24px' }}
  >
    <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
      <div>
        <Text style={{ fontSize: 13, color: '#6b7280', fontWeight: 500 }}>{title}</Text>
        <div style={{ fontSize: 28, fontWeight: 700, color: '#111827', lineHeight: 1.3, marginTop: 4 }}>
          {value.toLocaleString()}
          {suffix && <span style={{ fontSize: 14, color: '#9ca3af', marginLeft: 4 }}>{suffix}</span>}
        </div>
      </div>
      <div style={{
        width: 44, height: 44, borderRadius: 10,
        background: bg,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 20, color: color,
      }}>
        {icon}
      </div>
    </div>
  </Card>
);

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    ratesheetApi.stats().then(r => {
      setStats(r.data);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  if (loading) return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 300 }}>
      <Spin size="large" />
    </div>
  );

  const activeRate = stats ? Math.round((stats.activeRatesheets / Math.max(stats.totalRatesheets, 1)) * 100) : 0;

  return (
    <div>
      {/* Welcome banner */}
      <Card
        style={{
          borderRadius: 12, border: 'none', marginBottom: 20,
          background: 'linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%)',
          boxShadow: '0 4px 12px rgba(79,70,229,0.3)',
        }}
        bodyStyle={{ padding: '24px 28px' }}
      >
        <Row align="middle" justify="space-between">
          <Col>
            <Title level={3} style={{ color: '#fff', margin: 0, fontWeight: 700 }}>
              Welcome to BIL Platform 🌍
            </Title>
            <Text style={{ color: '#c4b5fd', fontSize: 14 }}>
              Digital Freight Management · Ocean Rates Intelligence
            </Text>
          </Col>
          <Col>
            <Tag color="rgba(255,255,255,0.2)" style={{ color: '#fff', border: '1px solid rgba(255,255,255,0.3)', borderRadius: 20, padding: '2px 12px', fontSize: 12 }}>
              April 2026
            </Tag>
          </Col>
        </Row>
      </Card>

      {/* KPI row */}
      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={12} lg={6}>
          <StatCard title="Carriers" value={stats?.carrierCount ?? 0} icon={<ShopOutlined />} color="#4f46e5" bg="#ede9fe" />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard title="Active Ratesheets" value={stats?.activeRatesheets ?? 0} icon={<CheckCircleOutlined />} color="#10b981" bg="#d1fae5" />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard title="Total Rate Lines" value={stats?.totalRateLines ?? 0} suffix="lines" icon={<TableOutlined />} color="#f59e0b" bg="#fef3c7" />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard title="Known Ports" value={stats?.portCount ?? 0} icon={<GlobalOutlined />} color="#3b82f6" bg="#dbeafe" />
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        {/* Ratesheet health */}
        <Col xs={24} lg={8}>
          <Card
            title={<><AimOutlined style={{ marginRight: 6, color: '#4f46e5' }} />Ratesheet Coverage</>}
            style={{ borderRadius: 12, border: 'none', boxShadow: '0 1px 3px rgba(0,0,0,0.08)', height: '100%' }}
          >
            <div style={{ textAlign: 'center', padding: '16px 0' }}>
              <Progress
                type="dashboard"
                percent={activeRate}
                strokeColor={{ '0%': '#4f46e5', '100%': '#10b981' }}
                strokeWidth={10}
                format={p => (
                  <div>
                    <div style={{ fontSize: 22, fontWeight: 700, color: '#111827' }}>{p}%</div>
                    <div style={{ fontSize: 11, color: '#9ca3af' }}>Active</div>
                  </div>
                )}
              />
            </div>
            <Divider style={{ margin: '12px 0' }} />
            <Row justify="space-around" style={{ textAlign: 'center' }}>
              <Col>
                <div style={{ fontSize: 20, fontWeight: 700, color: '#10b981' }}>{stats?.activeRatesheets ?? 0}</div>
                <div style={{ fontSize: 12, color: '#6b7280' }}>Active</div>
              </Col>
              <Col>
                <div style={{ fontSize: 20, fontWeight: 700, color: '#111827' }}>{stats?.totalRatesheets ?? 0}</div>
                <div style={{ fontSize: 12, color: '#6b7280' }}>Total</div>
              </Col>
            </Row>
          </Card>
        </Col>

        {/* Quick stats */}
        <Col xs={24} lg={16}>
          <Card
            title={<><RiseOutlined style={{ marginRight: 6, color: '#4f46e5' }} />Platform Summary</>}
            style={{ borderRadius: 12, border: 'none', boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}
          >
            <Row gutter={[0, 16]}>
              {[
                { label: 'Total Ratesheets', value: stats?.totalRatesheets ?? 0, color: '#4f46e5' },
                { label: 'Rate Lines Indexed', value: stats?.totalRateLines ?? 0, color: '#f59e0b' },
                { label: 'Ports in Database', value: stats?.portCount ?? 0, color: '#3b82f6' },
                { label: 'Carriers Loaded', value: stats?.carrierCount ?? 0, color: '#10b981' },
              ].map(item => (
                <Col xs={24} sm={12} key={item.label}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f3f4f6' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div style={{ width: 8, height: 8, borderRadius: '50%', background: item.color }} />
                      <Text style={{ color: '#374151', fontSize: 13 }}>{item.label}</Text>
                    </div>
                    <Text strong style={{ fontSize: 15, color: item.color }}>{item.value.toLocaleString()}</Text>
                  </div>
                </Col>
              ))}
            </Row>
            <div style={{
              marginTop: 20, padding: '12px 16px',
              background: 'linear-gradient(135deg, #f0fdf4, #ede9fe)',
              borderRadius: 8,
              display: 'flex', alignItems: 'center', gap: 10,
            }}>
              <CheckCircleOutlined style={{ color: '#10b981', fontSize: 16 }} />
              <Text style={{ fontSize: 13, color: '#374151' }}>
                System is <strong style={{ color: '#10b981' }}>operational</strong> · All parsers running · API ready
              </Text>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;

