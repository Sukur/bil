import React, { useState } from 'react';
import {
  Upload,
  Button,
  message,
  Card,
  Result,
  Typography,
  Progress,
} from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';
import { ratesheetApi } from '../api/ratesheetApi';
import type { ImportResult } from '../api/ratesheetApi';

const { Dragger } = Upload;
const { Text } = Typography;

const ImportPage: React.FC = () => {
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleUpload = async (file: File) => {
    setUploading(true);
    setResult(null);
    setError(null);
    try {
      const res = await ratesheetApi.import(file);
      setResult(res.data);
      message.success(`Imported ${res.data.linesImported} lines via ${res.data.parser}`);
    } catch (err: any) {
      const msg = err?.response?.data?.message || err.message || 'Upload failed';
      setError(msg);
      message.error(`Import failed: ${msg}`);
    } finally {
      setUploading(false);
    }
    return false; // prevent default upload
  };

  const props: UploadProps = {
    name: 'file',
    multiple: false,
    accept: '.xlsx,.xls',
    beforeUpload: handleUpload,
    showUploadList: false,
  };

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>Import Ratesheet</h2>
      <Card>
        <Dragger {...props} disabled={uploading} style={{ padding: 16 }}>
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">Click or drag Excel ratesheet here</p>
          <p className="ant-upload-hint">
            Supports HMM, ONE (AET/IET/ZFS), COSCO (IET/OOG/Reefer), OOCL, NCPE
          </p>
        </Dragger>

        {uploading && (
          <div style={{ marginTop: 16 }}>
            <Progress percent={99} status="active" />
            <Text type="secondary">Processing ratesheet…</Text>
          </div>
        )}

        {result && (
          <Result
            status="success"
            title={`Successfully imported ${result.linesImported} rate lines`}
            subTitle={
              <>
                <Text>Ratesheet ID: <strong>{result.ratesheetId}</strong></Text>
                <br />
                <Text>Parser: <strong>{result.parser}</strong></Text>
                <br />
                <Text>{result.message}</Text>
              </>
            }
          />
        )}

        {error && (
          <Result
            status="error"
            title="Import Failed"
            subTitle={error}
            extra={
              <Button type="primary" onClick={() => setError(null)}>
                Try Again
              </Button>
            }
          />
        )}
      </Card>
    </div>
  );
};

export default ImportPage;

