#!/usr/bin/env python3
"""Creates Quotation UI frontend files."""
import os

base = "/Users/shukurrzayev/Documents/bil/bil/frontend/src"

def w(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(content)
    print(f"  OK  {path}")

# ── quoteApi.ts ────────────────────────────────────────────────────────────────
w(f"{base}/api/quoteApi.ts", """\
import api from './config';

export interface QuoteRequest {
  pol: string;
  pod: string;
  equipment: string;
  commodity?: string;
  readyDate?: string;   // ISO yyyy-MM-dd
  weightKg?: number;
}

export interface QuoteCharge {
  code: string;
  description: string;
  amount: number;
  currency: string;
  basis: string;
}

export interface QuoteResponse {
  quoteId: number | null;
  found: boolean;
  pol: string;
  pod: string;
  equipment: string;
  commodity: string;
  readyDate: string;
  carrierScac: string;
  carrierName: string;
  oceanFreight: number;
  markupAmount: number;
  totalAmount: number;
  currency: string;
  validUntil: string;
  transitDays: number | null;
  candidateCount: number;
  charges: QuoteCharge[];
  createdAt: string;
  message: string | null;
}

export interface PortItem {
  unloc: string;
  name: string;
  country: string;
}

export interface CarrierItem {
  id: number;
  scac: string;
  name: string;
}

export const quoteApi = {
  create: (req: QuoteRequest) => api.post<QuoteResponse>('/quotes', req),
  get: (id: number) => api.get<QuoteResponse>(`/quotes/${id}`),
  list: (page = 0, size = 20) =>
    api.get('/quotes', { params: { page, size } }),
  searchPorts: (q: string) =>
    api.get<PortItem[]>('/ports', { params: { q } }),
  carriers: () => api.get<CarrierItem[]>('/carriers'),
};
""")

# ── QuotePage.tsx ──────────────────────────────────────────────────────────────
w(f"{base}/pages/QuotePage.tsx", """\
import React, { useState } from 'react';
import {
  Card, Form, Input, Select, Button, DatePicker, InputNumber,
  Divider, Table, Tag, Alert, Spin, Typography, Row, Col, Statistic,
} from 'antd';
import { CalculatorOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { quoteApi, QuoteResponse, QuoteCharge } from '../api/quoteApi';

const { Title, Text } = Typography;

const EQUIPMENT_OPTIONS = [
  { value: 'DC20', label: "20' Dry (DC20)" },
  { value: 'DC40', label: "40' Dry (DC40)" },
  { value: 'HC40', label: "40' High Cube (HC40)" },
  { value: 'RF20', label: "20' Reefer (RF20)" },
  { value: 'RF40', label: "40' Reefer (RF40)" },
  { value: 'HR40', label: "40' HC Reefer (HR40)" },
  { value: 'OT20', label: "20' Open Top (OT20)" },
  { value: 'OT40', label: "40' Open Top (OT40)" },
  { value: 'FR20', label: "20' Flat Rack (FR20)" },
  { value: 'FR40', label: "40' Flat Rack (FR40)" },
];

const chargeColumns: ColumnsType<QuoteCharge> = [
  { title: 'Code',        dataIndex: 'code',        width: 160, render: v => <Tag>{v}</Tag> },
  { title: 'Description', dataIndex: 'description', ellipsis: true },
  { title: 'Basis',       dataIndex: 'basis',       width: 110 },
  {
    title: 'Amount', dataIndex: 'amount', width: 140, align: 'right' as const,
    render: (v, r) => <strong>{r.currency} {Number(v).toLocaleString(undefined, { minimumFractionDigits: 2 })}</strong>,
  },
];

const QuotePage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<QuoteResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const onFinish = async (values: any) => {
    setLoading(true);
    setResult(null);
    setError(null);
    try {
      const req = {
        pol: (values.pol as string).trim().toUpperCase(),
        pod: (values.pod as string).trim().toUpperCase(),
        equipment: values.equipment,
        commodity: values.commodity || 'FAK',
        readyDate: values.readyDate ? dayjs(values.readyDate).format('YYYY-MM-DD') : undefined,
        weightKg: values.weightKg || undefined,
      };
      const res = await quoteApi.create(req);
      setResult(res.data);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Request failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Title level={4} style={{ marginBottom: 24 }}>Instant Ocean Freight Quote</Title>

      {/* ── Form ── */}
      <Card style={{ marginBottom: 24 }}>
        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
          initialValues={{ equipment: 'HC40', commodity: 'FAK' }}
          style={{ maxWidth: 700 }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="pol" label="Port of Loading (POL)" rules={[{ required: true, message: 'Enter POL UN/LOCODE' }]}>
                <Input placeholder="e.g. DEHAM" maxLength={5} style={{ textTransform: 'uppercase' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="pod" label="Port of Discharge (POD)" rules={[{ required: true, message: 'Enter POD UN/LOCODE' }]}>
                <Input placeholder="e.g. CNSHA" maxLength={5} style={{ textTransform: 'uppercase' }} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={10}>
              <Form.Item name="equipment" label="Equipment" rules={[{ required: true }]}>
                <Select options={EQUIPMENT_OPTIONS} />
              </Form.Item>
            </Col>
            <Col span={7}>
              <Form.Item name="commodity" label="Commodity">
                <Input placeholder="FAK" />
              </Form.Item>
            </Col>
            <Col span={7}>
              <Form.Item name="readyDate" label="Ready Date">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="weightKg" label="Cargo Weight (kg)">
                <InputNumber style={{ width: '100%' }} placeholder="e.g. 15000" min={1} />
              </Form.Item>
            </Col>
            <Col span={8} style={{ display: 'flex', alignItems: 'flex-end', paddingBottom: 24 }}>
              <Button
                type="primary"
                htmlType="submit"
                icon={<CalculatorOutlined />}
                loading={loading}
                size="large"
                block
              >
                Get Quote
              </Button>
            </Col>
          </Row>
        </Form>
      </Card>

      {/* ── Loading ── */}
      {loading && <Spin size="large" style={{ display: 'flex', justifyContent: 'center', margin: 40 }} />}

      {/* ── Error ── */}
      {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />}

      {/* ── No-rate result ── */}
      {result && !result.found && (
        <Alert
          type="warning"
          icon={<CloseCircleOutlined />}
          message="No rates found"
          description={result.message}
          showIcon
        />
      )}

      {/* ── Quote result ── */}
      {result && result.found && (
        <Card
          title={
            <span>
              <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 8 }} />
              Quote #{result.quoteId} — {result.pol} → {result.pod} [{result.equipment}]
            </span>
          }
        >
          <Row gutter={24} style={{ marginBottom: 24 }}>
            <Col span={6}>
              <Statistic
                title="Carrier"
                value={result.carrierScac}
                suffix={<Text type="secondary" style={{ fontSize: 12 }}>{result.carrierName}</Text>}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="Ocean Freight"
                prefix={result.currency}
                value={result.oceanFreight}
                precision={2}
                valueStyle={{ color: '#1677ff' }}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="Service Margin"
                prefix={result.currency}
                value={result.markupAmount}
                precision={2}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="Total"
                prefix={result.currency}
                value={result.totalAmount}
                precision={2}
                valueStyle={{ color: '#52c41a', fontWeight: 700 }}
              />
            </Col>
          </Row>

          <Row gutter={24} style={{ marginBottom: 24 }}>
            <Col span={6}>
              <Text type="secondary">Valid Until</Text>
              <br />
              <Text strong>{result.validUntil}</Text>
            </Col>
            <Col span={6}>
              <Text type="secondary">Transit Days</Text>
              <br />
              <Text strong>{result.transitDays ?? '—'}</Text>
            </Col>
            <Col span={6}>
              <Text type="secondary">Commodity</Text>
              <br />
              <Text strong>{result.commodity}</Text>
            </Col>
            <Col span={6}>
              <Text type="secondary">Candidates</Text>
              <br />
              <Text strong>{result.candidateCount} rate line(s) evaluated</Text>
            </Col>
          </Row>

          <Divider>Charge Breakdown</Divider>

          <Table
            columns={chargeColumns}
            dataSource={result.charges}
            rowKey="code"
            size="small"
            pagination={false}
          />
        </Card>
      )}
    </div>
  );
};

export default QuotePage;
""")

print("Frontend quote files created.")

