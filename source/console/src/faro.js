import { getWebInstrumentations, initializeFaro } from '@grafana/faro-web-sdk';
import { TracingInstrumentation } from '@grafana/faro-web-tracing';

let faro = null;

if (window.APP_CONFIG?.FARO_ENABLED !== false) {
  faro = initializeFaro({
    url: window.APP_CONFIG?.FARO_URL || 'https://faro-collector-prod-ap-southeast-1.grafana.net/collect/<app-key>/',
    app: {
      name: 'iso8583-console',
      version: '1.0.0',
      environment: window.APP_CONFIG?.ENVIRONMENT || 'development'
    },
    instrumentations: [
      ...getWebInstrumentations(),
      new TracingInstrumentation(),
    ],
  });
  console.log('✅ Grafana Faro initialized');
} else {
  console.log('⚠️ Grafana Faro disabled');
}

export default faro;