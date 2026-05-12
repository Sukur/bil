import React, { useState } from 'react';
import { ConfigProvider, Layout, Menu, Typography, Avatar, Badge } from 'antd';
import {
  DashboardOutlined,
  UploadOutlined,
  UnorderedListOutlined,
  SearchOutlined,
  CalculatorOutlined,
  GlobalOutlined,
  BellOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  TeamOutlined,
  BarChartOutlined,
  EllipsisOutlined,
} from '@ant-design/icons';
import Dashboard from './pages/Dashboard';
import ImportPage from './pages/ImportPage';
import RatesheetsPage from './pages/RatesheetsPage';
import RateSearchPage from './pages/RateSearchPage';
import QuotePage from './pages/QuotePage';
import CarriersPage from './pages/CarriersPage';
import ReportsPage from './pages/ReportsPage';
import 'antd/dist/reset.css';
import './index.css';

const { Header, Content, Sider } = Layout;
const { Text } = Typography;

type PageKey = 'dashboard' | 'import' | 'ratesheets' | 'search' | 'quote' | 'carriers' | 'reports';

const PAGE_LABELS: Record<PageKey, string> = {
  dashboard:  'Dashboard',
  quote:      'Get Quote',
  import:     'Import Ratesheet',
  ratesheets: 'Ratesheets',
  search:     'Rate Search',
  carriers:   'Carriers',
  reports:    'Reports',
};

const PAGES: Record<PageKey, React.ReactNode> = {
  dashboard:  <Dashboard />,
  import:     <ImportPage />,
  ratesheets: <RatesheetsPage />,
  search:     <RateSearchPage />,
  quote:      <QuotePage />,
  carriers:   <CarriersPage />,
  reports:    <ReportsPage />,
};

const App: React.FC = () => {
  const [page, setPage]         = useState<PageKey>('dashboard');
  const [collapsed, setCollapsed] = useState(false);

  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#4f46e5',
          colorSuccess: '#10b981',
          colorWarning: '#f59e0b',
          colorError:   '#ef4444',
          borderRadius: 8,
          fontFamily:   "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
        },
        components: {
          Layout: { headerBg: '#ffffff', siderBg: '#1e1b4b' },
          Menu:   { darkItemBg: 'transparent', darkSubMenuItemBg: 'transparent' },
        },
      }}
    >
      <Layout style={{ height: '100vh', overflow: 'hidden', background: '#f0f2f5' }}>

        {/* ── Sidebar — fixed, full height, independent scroll ── */}
        <Sider
          theme="dark"
          collapsible
          collapsed={collapsed}
          onCollapse={setCollapsed}
          trigger={null}
          width={220}
          style={{
            background: 'linear-gradient(180deg, #1e1b4b 0%, #312e81 100%)',
            boxShadow: '2px 0 8px rgba(0,0,0,0.15)',
            height: '100vh',
            position: 'fixed',
            left: 0,
            top: 0,
            bottom: 0,
            zIndex: 200,
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          {/* Logo */}
          <div style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            padding: collapsed ? '0' : '0 20px',
            borderBottom: '1px solid rgba(255,255,255,0.08)',
            gap: 10,
          }}>
            <div style={{
              width: 32, height: 32, borderRadius: 8,
              background: 'linear-gradient(135deg, #818cf8, #6366f1)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              flexShrink: 0, fontSize: 16, fontWeight: 700, color: '#fff',
            }}>
              B
            </div>
            {!collapsed && (
              <div>
                <div style={{ color: '#fff', fontWeight: 700, fontSize: 15, lineHeight: 1.2 }}>BIL</div>
                <div style={{ color: '#a5b4fc', fontSize: 10, lineHeight: 1 }}>Digital Freight</div>
              </div>
            )}
          </div>

          {/* Nav */}
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={[page]}
            onClick={({ key }) => setPage(key as PageKey)}
            style={{ background: 'transparent', marginTop: 8 }}
            items={[
              { key: 'dashboard',  icon: <DashboardOutlined />,      label: 'Dashboard' },
              { key: 'quote',      icon: <CalculatorOutlined />,      label: 'Get Quote' },
              { type: 'divider' } as any,
              { key: 'import',     icon: <UploadOutlined />,          label: 'Import Ratesheet' },
              { key: 'ratesheets', icon: <UnorderedListOutlined />,   label: 'Ratesheets' },
              { key: 'search',     icon: <SearchOutlined />,          label: 'Rate Search' },
              { type: 'divider' } as any,
              {
                key: 'more',
                icon: <EllipsisOutlined />,
                label: 'More Options',
                children: [
                  { key: 'carriers', icon: <TeamOutlined />,     label: 'Carriers' },
                  { key: 'reports',  icon: <BarChartOutlined />, label: 'Reports' },
                ],
              },
            ]}
          />

          {/* Bottom user area */}
          {!collapsed && (
            <div style={{
              position: 'absolute', bottom: 56, left: 0, right: 0,
              padding: '12px 16px',
              borderTop: '1px solid rgba(255,255,255,0.08)',
              display: 'flex', alignItems: 'center', gap: 10,
            }}>
              <Avatar size={30} style={{ background: '#818cf8', flexShrink: 0 }}>
                <GlobalOutlined />
              </Avatar>
              <div>
                <div style={{ color: '#e0e7ff', fontSize: 12, fontWeight: 600 }}>Forwarder</div>
                <div style={{ color: '#818cf8', fontSize: 10 }}>Hamburg, DE</div>
              </div>
            </div>
          )}
        </Sider>

        {/* ── Right side — offset for fixed sidebar, scrolls vertically ── */}
        <Layout style={{
          marginLeft: collapsed ? 80 : 220,
          transition: 'margin-left 0.2s',
          height: '100vh',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}>
          {/* ── Header ── */}
          <Header style={{
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 0 #f0f0f0',
            height: 64,
            flexShrink: 0,
            zIndex: 100,
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
              <div
                onClick={() => setCollapsed(!collapsed)}
                style={{ cursor: 'pointer', fontSize: 18, color: '#6b7280', padding: '4px 8px', borderRadius: 6 }}
              >
                {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              </div>
              <div>
                <Text style={{ fontSize: 17, fontWeight: 600, color: '#111827' }}>
                  {PAGE_LABELS[page]}
                </Text>
                <div style={{ fontSize: 12, color: '#9ca3af', lineHeight: 1.2 }}>
                  BIL Platform · Ocean Freight
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
              <Badge count={0} showZero={false}>
                <BellOutlined style={{ fontSize: 18, color: '#6b7280', cursor: 'pointer' }} />
              </Badge>
              <div style={{ width: 1, height: 24, background: '#e5e7eb' }} />
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Avatar size={32} style={{ background: 'linear-gradient(135deg, #818cf8, #6366f1)', cursor: 'pointer' }}>
                  FF
                </Avatar>
                {!collapsed && (
                  <div style={{ display: 'none' }}>
                    <div style={{ fontSize: 13, fontWeight: 600, color: '#111827', lineHeight: 1.2 }}>Freight Fwd</div>
                    <div style={{ fontSize: 11, color: '#6b7280' }}>Admin</div>
                  </div>
                )}
              </div>
            </div>
          </Header>

          {/* ── Content — this is the only scrolling region ── */}
          <Content style={{
            flex: 1,
            overflowY: 'auto',
            overflowX: 'hidden',
            padding: 24,
            background: '#f0f2f5',
          }}>
            {PAGES[page]}
          </Content>
        </Layout>
      </Layout>
    </ConfigProvider>
  );
};

export default App;
