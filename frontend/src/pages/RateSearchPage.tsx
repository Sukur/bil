import React, { useState } from 'react';
import {
  Card, Form, Input, Select, Button, Table, Tag, Space,
} from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { ratesheetApi } from '../api/ratesheetApi';
import type { RateLine, Page } from '../api/ratesheetApi';

const EQUIPMENT_OPTIONS = [
  { value: '', label: 'All equipment' },
  { value: 'DC20', label: '20\' Dry (DC20)' },
  { value: 'DC40', label: '40\' Dry (DC40)' },
  { value: 'HC40', label: '40\' High Cube (HC40)' },
  { value: 'RF20', label: '20\' Reefer (RF20)' },
  { value: 'RF40', label: '40\' Reefer (RF40)' },
  { value: 'HR40', label: '40\' HC Reefer (HR40)' },
  { value: 'OT20', label: '20\' Open Top (OT20)' },
  { value: 'OT40', label: '40\' Open Top (OT40)' },
  { value: 'FR20', label: '20\' Flat Rack (FR20)' },
  { value: 'FR40', label: '40\' Flat Rack (FR40)' },
];

const RateSearchPage: React.FC = () => {
  const [form] = Form.useForm();
  const [results, setResults] = useState<RateLine[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  const search = async (values: any, p = 0) => {
    setLoading(true);
    try {
      const res = await ratesheetApi.search(
        values.pol || undefined,
        values.pod || undefined,
        values.equipment || undefined,
        p, 50
      );
      const data = res.data as Page<RateLine>;
      setResults(data.content);
      setTotal(data.totalElements);
      setPage(p);
    } finally {
      setLoading(false);
    }
  };

  const columns: ColumnsType<RateLine> = [
    { title: 'POL', dataIndex: 'pol', width: 75, render: (v, r) => <><strong>{v}</strong>{r.polName ? <><br /><small>{r.polName}</small></> : null}</> },
    { title: 'POD', dataIndex: 'pod', width: 75, render: (v, r) => <><strong>{v}</strong>{r.podName ? <><br /><small>{r.podName}</small></> : null}</> },
    { title: 'Via', dataIndex: 'via', width: 75, render: v => v || '—' },
    { title: 'Equipment', dataIndex: 'equipment', width: 100, render: v => <Tag>{v}</Tag> },
    { title: 'Commodity', dataIndex: 'commodity', width: 90 },
    {
      title: 'Rate', dataIndex: 'baseAmount', width: 110, sorter: (a, b) => a.baseAmount - b.baseAmount,
      render: (v, r) => <strong style={{ color: '#1677ff' }}>{r.currency} {Number(v).toLocaleString()}</strong>
    },
    { title: 'Days', dataIndex: 'transitDays', width: 65, render: v => v ?? '—' },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>Rate Search</h2>
      <Card style={{ marginBottom: 16 }}>
        <Form layout="inline" form={form} onFinish={v => search(v)}>
          <Form.Item name="pol" label="POL">
            <Input placeholder="e.g. DEHAM" allowClear style={{ width: 130 }} />
          </Form.Item>
          <Form.Item name="pod" label="POD">
            <Input placeholder="e.g. CNSHA" allowClear style={{ width: 130 }} />
          </Form.Item>
          <Form.Item name="equipment" label="Equipment">
            <Select options={EQUIPMENT_OPTIONS} style={{ width: 180 }} defaultValue="" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} htmlType="submit" loading={loading}>
                Search
              </Button>
              <Button
                onClick={() => {
                  form.resetFields();
                  setResults([]);
                  setTotal(0);
                }}
              >
                Clear
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      {total > 0 && (
        <Table
          columns={columns}
          dataSource={results}
          rowKey="id"
          loading={loading}
          size="small"
          pagination={{
            total,
            pageSize: 50,
            current: page + 1,
            onChange: (p) => search(form.getFieldsValue(), p - 1),
            showTotal: t => `${t} rate lines found`,
          }}
        />
      )}

      {!loading && total === 0 && results.length === 0 && (
        <Card style={{ textAlign: 'center', color: '#999' }}>
          Enter search criteria above and click Search
        </Card>
      )}
    </div>
  );
};

export default RateSearchPage;

