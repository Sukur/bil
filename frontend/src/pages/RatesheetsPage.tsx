import React, { useEffect, useState } from 'react';
import { Table, Tag, Space, Button, Spin, Input, Modal } from 'antd';
import { EyeOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { ratesheetApi, RatesheetSummary, RateLine, Page } from '../api/ratesheetApi';

const { Search } = Input;

const statusColor: Record<string, string> = {
  ACTIVE: 'green',
  EXPIRED: 'red',
  DRAFT: 'orange',
};

const typeColor: Record<string, string> = {
  FAK: 'blue',
  OOG: 'purple',
  REEFER: 'cyan',
  NAC: 'geekblue',
  SPOT: 'gold',
};

const RatesheetsPage: React.FC = () => {
  const [data, setData] = useState<RatesheetSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [lines, setLines] = useState<RateLine[]>([]);
  const [lineTotal, setLineTotal] = useState(0);
  const [linePage, setLinePage] = useState(0);
  const [linesLoading, setLinesLoading] = useState(false);
  const [filterText, setFilterText] = useState('');

  const load = () => {
    setLoading(true);
    ratesheetApi.list().then(r => {
      setData(r.data);
      setLoading(false);
    }).catch(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const openLines = (id: number, page = 0) => {
    setSelectedId(id);
    setModalOpen(true);
    setLinesLoading(true);
    ratesheetApi.lines(id, page, 50).then(r => {
      const p = r.data as Page<RateLine>;
      setLines(p.content);
      setLineTotal(p.totalElements);
      setLinePage(page);
      setLinesLoading(false);
    }).catch(() => setLinesLoading(false));
  };

  const filtered = data.filter(rs =>
    filterText === '' ||
    rs.carrierScac.toLowerCase().includes(filterText.toLowerCase()) ||
    rs.source.toLowerCase().includes(filterText.toLowerCase()) ||
    rs.status.toLowerCase().includes(filterText.toLowerCase())
  );

  const columns: ColumnsType<RatesheetSummary> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: 'Carrier', dataIndex: 'carrierScac', width: 80,
      render: (v, r) => <Tag color="blue">{v}</Tag> },
    { title: 'Source', dataIndex: 'source', ellipsis: true },
    { title: 'Type', dataIndex: 'type', width: 80,
      render: v => <Tag color={typeColor[v] || 'default'}>{v}</Tag> },
    { title: 'Currency', dataIndex: 'currency', width: 80 },
    { title: 'Valid From', dataIndex: 'validFrom', width: 110 },
    { title: 'Valid To', dataIndex: 'validTo', width: 110 },
    { title: 'Status', dataIndex: 'status', width: 90,
      render: v => <Tag color={statusColor[v] || 'default'}>{v}</Tag> },
    { title: 'Rate Lines', dataIndex: 'rateLineCount', width: 100,
      render: v => v?.toLocaleString() },
    {
      title: 'Action', width: 80,
      render: (_, r) => (
        <Button icon={<EyeOutlined />} size="small" onClick={() => openLines(r.id)}>
          Lines
        </Button>
      ),
    },
  ];

  const lineColumns: ColumnsType<RateLine> = [
    { title: 'POL', dataIndex: 'pol', width: 70 },
    { title: 'POD', dataIndex: 'pod', width: 70 },
    { title: 'Via', dataIndex: 'via', width: 70, render: v => v || '—' },
    { title: 'Equipment', dataIndex: 'equipment', width: 90,
      render: v => <Tag>{v}</Tag> },
    { title: 'Commodity', dataIndex: 'commodity', width: 90 },
    { title: 'Amount', dataIndex: 'baseAmount', width: 90,
      render: (v, r) => `${r.currency} ${Number(v).toLocaleString()}` },
    { title: 'Transit Days', dataIndex: 'transitDays', width: 100, render: v => v ?? '—' },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>Ratesheets</h2>
        <Search
          placeholder="Filter by carrier / source / status"
          onSearch={setFilterText}
          onChange={e => setFilterText(e.target.value)}
          style={{ width: 300 }}
          allowClear
        />
        <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
      </Space>

      <Table
        columns={columns}
        dataSource={filtered}
        rowKey="id"
        loading={loading}
        size="small"
        pagination={{ pageSize: 20, showTotal: t => `${t} ratesheets` }}
      />

      <Modal
        title={`Rate Lines — Ratesheet #${selectedId}`}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
        width={900}
      >
        <Table
          columns={lineColumns}
          dataSource={lines}
          rowKey="id"
          loading={linesLoading}
          size="small"
          pagination={{
            total: lineTotal,
            pageSize: 50,
            current: linePage + 1,
            onChange: (p) => selectedId && openLines(selectedId, p - 1),
            showTotal: t => `${t} lines`,
          }}
        />
      </Modal>
    </div>
  );
};

export default RatesheetsPage;

