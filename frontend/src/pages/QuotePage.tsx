import React, { useState, useCallback } from 'react';
import {
  Card, Form, Select, Button, DatePicker,
  Tag, Alert, Spin, Typography, Row, Col, Divider, Tooltip, Empty,
} from 'antd';
import {
  CalculatorOutlined, CheckCircleOutlined,
  SwapRightOutlined, GlobalOutlined, MailOutlined, ThunderboltOutlined,
  DollarOutlined, StarOutlined, TrophyOutlined, ClockCircleOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { quoteApi } from '../api/quoteApi';
import type { QuoteCompareResponse, QuoteOption, PortItem } from '../api/quoteApi';

const { Title, Text } = Typography;

/* ─── Searchable port autocomplete ─────────────────────────────────────────── */
interface PortSelectProps {
  value?: string;
  onChange?: (val: string) => void;
  placeholder?: string;
}

const PortSelect: React.FC<PortSelectProps> = ({ value, onChange, placeholder }) => {
  const [options, setOptions] = useState<{ value: string; label: React.ReactNode }[]>([]);
  const [loading, setLoading] = useState(false);
  const timer = React.useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  const handleSearch = useCallback((q: string) => {
    clearTimeout(timer.current);
    if (!q || q.length < 2) { setOptions([]); return; }
    timer.current = setTimeout(async () => {
      setLoading(true);
      try {
        const res = await quoteApi.searchPorts(q);
        setOptions(
          res.data.map((p: PortItem) => ({
            value: p.unloc,
            label: (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <span style={{ fontFamily: 'monospace', fontWeight: 700, color: '#4f46e5', marginRight: 8 }}>
                    {p.unloc}
                  </span>
                  <span style={{ color: '#374151', fontSize: 13 }}>{p.name}</span>
                </div>
                <Tag style={{ fontSize: 10, marginLeft: 8, flexShrink: 0 }}>{p.country}</Tag>
              </div>
            ),
          }))
        );
      } finally {
        setLoading(false);
      }
    }, 250);
  }, []);

  return (
    <Select
      showSearch
      value={value}
      onChange={onChange}
      placeholder={placeholder}
      filterOption={false}
      onSearch={handleSearch}
      notFoundContent={loading ? <Spin size="small" /> : <span style={{ color: '#9ca3af', fontSize: 12 }}>Type to search ports…</span>}
      options={options}
      size="large"
      suffixIcon={<GlobalOutlined style={{ color: '#9ca3af' }} />}
      style={{ width: '100%' }}
      dropdownStyle={{ minWidth: 320 }}
      optionLabelProp="value"
    />
  );
};

/* ─── Equipment options ─────────────────────────────────────────────────────── */
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

/* ─── Tag config ──────────────────────────────────────────────────────────── */
const TAG_CONFIG: Record<string, { icon: React.ReactNode; color: string; gradient: string; border: string }> = {
  'CHEAPEST':    { icon: <DollarOutlined />,     color: '#10b981', gradient: 'linear-gradient(135deg,#ecfdf5,#d1fae5)', border: '#6ee7b7' },
  'FASTEST':     { icon: <ThunderboltOutlined />, color: '#f59e0b', gradient: 'linear-gradient(135deg,#fffbeb,#fef3c7)', border: '#fcd34d' },
  'BEST VALUE':  { icon: <TrophyOutlined />,      color: '#4f46e5', gradient: 'linear-gradient(135deg,#ede9fe,#ddd6fe)', border: '#a78bfa' },
  'RECOMMENDED': { icon: <StarOutlined />,         color: '#3b82f6', gradient: 'linear-gradient(135deg,#eff6ff,#dbeafe)', border: '#93c5fd' },
  '':            { icon: null,                     color: '#6b7280', gradient: '#f9fafb',                                   border: '#e5e7eb' },
};

/* ─── Email builder ───────────────────────────────────────────────────────── */
function buildMailto(result: QuoteCompareResponse, selected: QuoteOption): string {
  const subject = `Ocean Freight Quote – ${result.pol} → ${result.pod} [${result.equipment}]`;
  const fmt = (n: number) => n.toLocaleString('en', { minimumFractionDigits: 2 });
  const sep = '─'.repeat(44);

  const otherOptions = result.options
    .filter(o => o.carrierScac !== selected.carrierScac)
    .slice(0, 3)
    .map(o => `  • ${o.carrierScac.padEnd(6)} ${o.currency} ${fmt(o.totalAmount)}${o.transitDays ? `  (${o.transitDays}d)` : ''}`)
    .join('\n');

  const body = `Dear Client,

Please find below our ocean freight quotation:

${sep}
ROUTE       : ${result.pol} → ${result.pod}
EQUIPMENT   : ${result.equipment}
COMMODITY   : ${result.commodity}
READY DATE  : ${result.readyDate}
${sep}

RECOMMENDED OPTION  ${selected.tag ? `[${selected.tag}]` : ''}
Carrier             : ${selected.carrierName} (${selected.carrierScac})
Ocean Freight       : ${selected.currency} ${fmt(selected.oceanFreight)}
Service Margin (10%): ${selected.currency} ${fmt(selected.markupAmount)}
${sep}
TOTAL PRICE         : ${selected.currency} ${fmt(selected.totalAmount)}${selected.transitDays ? `\nTransit Time        : ${selected.transitDays} days` : ''}
${sep}

${otherOptions ? `OTHER OPTIONS:\n${otherOptions}\n\n` : ''}This quotation is based on current carrier tariffs and subject to space availability.
Rates are valid for 7 days from today.

To confirm this booking, please reply to this email with your cargo details.

Best regards,
BIL Digital Freight Team`;

  return `mailto:?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
}

/* ─── Option card ─────────────────────────────────────────────────────────── */
const OptionCard: React.FC<{
  option: QuoteOption;
  isSelected: boolean;
  onSelect: () => void;
  rank: number;
}> = ({ option, isSelected, onSelect, rank }) => {
  const cfg = TAG_CONFIG[option.tag] ?? TAG_CONFIG[''];
  const fmt = (n: number) => n.toLocaleString('en', { minimumFractionDigits: 2 });

  return (
    <div
      onClick={onSelect}
      style={{
        border: `2px solid ${isSelected ? cfg.color : '#e5e7eb'}`,
        borderRadius: 14,
        cursor: 'pointer',
        background: isSelected ? cfg.gradient : '#fff',
        transition: 'all 0.2s',
        position: 'relative',
        overflow: 'hidden',
        boxShadow: isSelected ? `0 4px 16px ${cfg.color}33` : '0 1px 3px rgba(0,0,0,0.06)',
      }}
    >
      {/* Top badge */}
      {option.tag && (
        <div style={{
          background: cfg.color,
          color: '#fff', fontSize: 10, fontWeight: 700, letterSpacing: 1,
          padding: '4px 12px', display: 'inline-flex', alignItems: 'center', gap: 4,
          borderRadius: '12px 0 12px 0', position: 'absolute', top: 0, left: 0,
        }}>
          {cfg.icon} {option.tag}
        </div>
      )}

      <div style={{ padding: option.tag ? '36px 20px 20px' : '20px' }}>
        {/* Carrier */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
          <div>
            <div style={{ fontWeight: 800, fontSize: 18, color: '#111827' }}>{option.carrierScac}</div>
            <div style={{ fontSize: 11, color: '#9ca3af', marginTop: 1 }}>{option.carrierName}</div>
          </div>
          {rank === 0 && <Tag color="gold" style={{ fontSize: 10 }}>BEST PRICE</Tag>}
        </div>

        <Divider style={{ margin: '10px 0' }} />

        {/* Price rows */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <Text style={{ fontSize: 12, color: '#6b7280' }}>Ocean Freight</Text>
            <Text style={{ fontSize: 13, fontWeight: 600, color: '#374151' }}>
              {option.currency} {fmt(option.oceanFreight)}
            </Text>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <Text style={{ fontSize: 12, color: '#6b7280' }}>Service Margin</Text>
            <Text style={{ fontSize: 13, color: '#374151' }}>
              + {option.currency} {fmt(option.markupAmount)}
            </Text>
          </div>
          <div style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            marginTop: 4, padding: '10px 12px', borderRadius: 8,
            background: isSelected ? cfg.color : '#f3f4f6',
          }}>
            <Text style={{ fontSize: 12, fontWeight: 700, color: isSelected ? '#fff' : '#374151' }}>TOTAL</Text>
            <Text style={{ fontSize: 18, fontWeight: 800, color: isSelected ? '#fff' : cfg.color }}>
              {option.currency} {fmt(option.totalAmount)}
            </Text>
          </div>
        </div>

        {/* Transit */}
        {option.transitDays && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginTop: 10, color: '#9ca3af', fontSize: 12 }}>
            <ClockCircleOutlined />
            <span>~{option.transitDays} days transit</span>
          </div>
        )}

        {/* Select indicator */}
        <div style={{ marginTop: 12, textAlign: 'center' }}>
          {isSelected
            ? <Text style={{ fontSize: 12, color: cfg.color, fontWeight: 700 }}>✓ Selected</Text>
            : <Text style={{ fontSize: 12, color: '#9ca3af' }}>Click to select</Text>}
        </div>
      </div>
    </div>
  );
};

/* ── Page ────────────────────────────────────────────────────────────────── */
const QuotePage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading]             = useState(false);
  const [result, setResult]               = useState<QuoteCompareResponse | null>(null);
  const [selectedIdx, setSelectedIdx]     = useState(0);
  const [error, setError]                 = useState<string | null>(null);

  const onFinish = async (values: any) => {
    setLoading(true); setResult(null); setError(null); setSelectedIdx(0);
    try {
      const req = {
        pol:       (values.pol as string).trim().toUpperCase(),
        pod:       (values.pod as string).trim().toUpperCase(),
        equipment: values.equipment,
        commodity: values.commodity || 'FAK',
        readyDate: values.readyDate ? dayjs(values.readyDate).format('YYYY-MM-DD') : undefined,
      };
      const res = await quoteApi.compare(req);
      setResult(res.data);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Request failed');
    } finally { setLoading(false); }
  };

  const selected = result?.options[selectedIdx];

  return (
    <div>
      {/* ── Form ── */}
      <Card style={{ borderRadius: 12, border: 'none', boxShadow: '0 1px 3px rgba(0,0,0,0.08)', marginBottom: 20 }} bodyStyle={{ padding: 28 }}>
        <div style={{ marginBottom: 20 }}>
          <Title level={4} style={{ margin: 0, color: '#111827' }}>Instant Ocean Freight Quote</Title>
          <Text type="secondary" style={{ fontSize: 13 }}>
            Compare rates across all carriers — cheapest, fastest and best-value options.
          </Text>
        </div>
        <Form form={form} layout="vertical" onFinish={onFinish} initialValues={{ equipment: 'HC40', commodity: 'FAK' }}>
          <Row gutter={16} align="bottom">
            <Col xs={24} sm={11}>
              <Form.Item name="pol" label="Port of Loading (POL)" rules={[{ required: true, message: 'Select a port' }]}>
                <PortSelect placeholder="Search: Hamburg, DEHAM…" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={2} style={{ textAlign: 'center', paddingBottom: 24 }}>
              <SwapRightOutlined style={{ fontSize: 22, color: '#d1d5db' }} />
            </Col>
            <Col xs={24} sm={11}>
              <Form.Item name="pod" label="Port of Discharge (POD)" rules={[{ required: true, message: 'Select a port' }]}>
                <PortSelect placeholder="Search: Shanghai, CNSHA…" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16} align="bottom">
            <Col xs={24} sm={9}>
              <Form.Item name="equipment" label="Equipment Type" rules={[{ required: true }]}>
                <Select options={EQUIPMENT_OPTIONS} size="large" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={5}>
              <Form.Item name="commodity" label="Commodity">
                <Select size="large" options={[
                  { value: 'FAK',    label: 'FAK – Freight All Kinds' },
                  { value: 'OOG',    label: 'OOG – Out of Gauge' },
                  { value: 'REEFER', label: 'Reefer / Perishables' },
                  { value: 'HAZMAT', label: 'Hazmat / DG' },
                ]} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={5}>
              <Form.Item name="readyDate" label="Cargo Ready Date">
                <DatePicker style={{ width: '100%' }} size="large" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={5} style={{ display: 'flex', alignItems: 'flex-end', paddingBottom: 24 }}>
              <Button
                type="primary" htmlType="submit" icon={<CalculatorOutlined />}
                loading={loading} size="large" block
                style={{ background: 'linear-gradient(135deg,#4f46e5,#7c3aed)', border: 'none', fontWeight: 600 }}
              >
                Compare Rates
              </Button>
            </Col>
          </Row>
        </Form>
      </Card>

      {/* ── Loading ── */}
      {loading && (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
          <div style={{ marginTop: 12, color: '#6b7280', fontSize: 14 }}>Searching across all carriers…</div>
        </div>
      )}

      {/* ── Error ── */}
      {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16, borderRadius: 10 }} />}

      {/* ── No rates ── */}
      {result && result.options.length === 0 && (
        <Card style={{ borderRadius: 12, border: 'none', textAlign: 'center' }} bodyStyle={{ padding: 48 }}>
          <Empty description={
            <span>No active rates found for <strong>{result.pol} → {result.pod}</strong> [{result.equipment}] on {result.readyDate}</span>
          } />
        </Card>
      )}

      {/* ── Results ── */}
      {result && result.options.length > 0 && (
        <div>
          {/* Result header */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <div>
              <Text strong style={{ fontSize: 16, color: '#111827' }}>
                <CheckCircleOutlined style={{ color: '#10b981', marginRight: 6 }} />
                {result.options.length} carrier option{result.options.length > 1 ? 's' : ''} found
              </Text>
              <div style={{ fontSize: 12, color: '#9ca3af', marginTop: 2 }}>
                {result.pol} → {result.pod} · {result.equipment} · {result.totalCandidates} rate lines evaluated
              </div>
            </div>
            {/* Email button */}
            {selected && (
              <Tooltip title="Opens your default email app with a pre-filled quote template">
                <Button
                  icon={<MailOutlined />}
                  size="large"
                  style={{
                    background: 'linear-gradient(135deg,#4f46e5,#7c3aed)',
                    border: 'none', color: '#fff', fontWeight: 600,
                    boxShadow: '0 2px 8px rgba(79,70,229,0.4)',
                  }}
                  onClick={() => { window.location.href = buildMailto(result, selected); }}
                >
                  Send Quote by Email
                </Button>
              </Tooltip>
            )}
          </div>

          {/* Option cards */}
          <Row gutter={[16, 16]}>
            {result.options.map((opt, i) => (
              <Col xs={24} sm={12} lg={8} key={opt.carrierScac + i}>
                <OptionCard
                  option={opt}
                  isSelected={selectedIdx === i}
                  onSelect={() => setSelectedIdx(i)}
                  rank={i}
                />
              </Col>
            ))}
          </Row>

          {/* Selected detail */}
          {selected && (
            <Card
              style={{ marginTop: 20, borderRadius: 12, border: '2px solid #4f46e5', boxShadow: '0 4px 16px rgba(79,70,229,0.12)' }}
              bodyStyle={{ padding: '20px 28px' }}
            >
              <Row justify="space-between" align="middle" style={{ marginBottom: 12 }}>
                <Col>
                  <Text strong style={{ fontSize: 15, color: '#111827' }}>
                    Selected: {selected.carrierName} ({selected.carrierScac})
                  </Text>
                  <div style={{ fontSize: 12, color: '#9ca3af' }}>
                    {result.pol} → {result.pod} · {result.equipment} · {result.commodity}
                  </div>
                </Col>
                <Col>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: 24, fontWeight: 800, color: '#4f46e5' }}>
                      {selected.currency} {selected.totalAmount.toLocaleString('en', { minimumFractionDigits: 2 })}
                    </div>
                    <div style={{ fontSize: 11, color: '#9ca3af' }}>
                      Base {selected.currency} {selected.oceanFreight.toLocaleString('en', { minimumFractionDigits: 2 })} + 10% margin
                    </div>
                  </div>
                </Col>
              </Row>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {selected.tags.map(t => (
                  <Tag key={t} color={t === 'CHEAPEST' ? 'green' : t === 'FASTEST' ? 'gold' : 'blue'} style={{ fontSize: 12, padding: '2px 10px' }}>
                    {TAG_CONFIG[t]?.icon} {t}
                  </Tag>
                ))}
                {selected.transitDays && (
                  <Tag icon={<ClockCircleOutlined />} color="default">~{selected.transitDays} days transit</Tag>
                )}
              </div>
            </Card>
          )}
        </div>
      )}
    </div>
  );
};

export default QuotePage;
