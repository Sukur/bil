import React from 'react';
import { Card, Table, Tag, Typography, Space, Avatar } from 'antd';


const { Title, Text } = Typography;

const carriers = [
  { key: '1', name: 'COSCO Shipping Lines', code: 'COSCO', region: 'Asia / Europe', status: 'active', rates: 42 },
  { key: '2', name: 'ONE (Ocean Network Express)', code: 'ONE', region: 'Global', status: 'active', rates: 35 },
  { key: '3', name: 'HMM', code: 'HMM', region: 'Asia / Europe', status: 'active', rates: 28 },
  { key: '4', name: 'OOCL', code: 'OOCL', region: 'Asia / Europe / Americas', status: 'active', rates: 19 },
  { key: '5', name: 'Hapag-Lloyd', code: 'HLCU', region: 'Global', status: 'inactive', rates: 0 },
  { key: '6', name: 'MSC', code: 'MSC', region: 'Global', status: 'inactive', rates: 0 },
];

const columns = [
  {
    title: 'Carrier',
    dataIndex: 'name',
    key: 'name',
    render: (name: string, row: any) => (
      <Space>
        <Avatar size={32} style={{ background: '#6366f1', fontSize: 11 }}>{row.code.slice(0, 2)}</Avatar>
        <div>
          <div style={{ fontWeight: 600, fontSize: 13 }}>{name}</div>
          <div style={{ fontSize: 11, color: '#9ca3af' }}>{row.code}</div>
        </div>
      </Space>
    ),
  },
  { title: 'Trade Region', dataIndex: 'region', key: 'region', render: (r: string) => <Text style={{ fontSize: 13 }}>{r}</Text> },
  {
    title: 'Active Rates',
    dataIndex: 'rates',
    key: 'rates',
    render: (r: number) => <Tag color={r > 0 ? 'blue' : 'default'}>{r} rates</Tag>,
  },
  {
    title: 'Status',
    dataIndex: 'status',
    key: 'status',
    render: (s: string) => <Tag color={s === 'active' ? 'success' : 'default'}>{s}</Tag>,
  },
];

const CarriersPage: React.FC = () => (
  <div>
    <div style={{ marginBottom: 20 }}>
      <Title level={4} style={{ margin: 0 }}>Carriers</Title>
      <Text type="secondary">All shipping carriers and their rate coverage</Text>
    </div>
    <Card>
      <Table dataSource={carriers} columns={columns} pagination={false} size="middle" />
    </Card>
  </div>
);

export default CarriersPage;


