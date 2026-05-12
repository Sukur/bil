import React from 'react';
import { Card, Row, Col, Statistic, Table, Tag, Typography } from 'antd';

const { Title, Text } = Typography;

const reportData = [
  { key: '1', month: 'April 2026', quotes: 48, imports: 10, carriers: 4, coverage: 87 },
  { key: '2', month: 'March 2026', quotes: 61, imports: 8, carriers: 4, coverage: 82 },
  { key: '3', month: 'February 2026', quotes: 39, imports: 6, carriers: 3, coverage: 74 },
  { key: '4', month: 'January 2026', quotes: 22, imports: 5, carriers: 2, coverage: 60 },
];

const columns = [
  { title: 'Month', dataIndex: 'month', key: 'month', render: (m: string) => <Text strong>{m}</Text> },
  { title: 'Quotes Generated', dataIndex: 'quotes', key: 'quotes', render: (n: number) => <Tag color="blue">{n}</Tag> },
  { title: 'Ratesheets Imported', dataIndex: 'imports', key: 'imports', render: (n: number) => <Tag color="purple">{n}</Tag> },
  { title: 'Active Carriers', dataIndex: 'carriers', key: 'carriers' },
  { title: 'Port Coverage (%)', dataIndex: 'coverage', key: 'coverage' },
];

const ReportsPage: React.FC = () => (
  <div>
    <div style={{ marginBottom: 20 }}>
      <Title level={4} style={{ margin: 0 }}>Reports</Title>
      <Text type="secondary">Monthly activity and performance overview</Text>
    </div>
    <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
      <Col span={6}><Card><Statistic title="Total Quotes (2026)" value={170} styles={{ title: { color: '#6366f1' } }} /></Card></Col>
      <Col span={6}><Card><Statistic title="Ratesheets Imported" value={29} styles={{ title: { color: '#10b981' } }} /></Card></Col>
      <Col span={6}><Card><Statistic title="Avg. Rates / Sheet" value={31} styles={{ title: { color: '#f59e0b' } }} /></Card></Col>
      <Col span={6}><Card><Statistic title="Active Carriers" value={4} styles={{ title: { color: '#6366f1' } }} /></Card></Col>
    </Row>
    <Card>
      <Table dataSource={reportData} columns={columns} pagination={false} size="middle" />
    </Card>
  </div>
);

export default ReportsPage;
