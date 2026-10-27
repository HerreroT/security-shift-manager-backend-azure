// Application Insights (sobre log-shiftmanager) + Workbook "Shiftmanager - Métricas" con todas las métricas juntas.
// Deploy: az deployment group create -g rg-shiftmanager -f infra/monitoring.bicep
// Ver docs/monitoreo.md, sección "Application Insights".

param location string = resourceGroup().location
param workspaceName string = 'log-shiftmanager'
param containerAppName string = 'ca-shiftmanager-backend'
param appInsightsName string = 'appi-shiftmanager'

resource workspace 'Microsoft.OperationalInsights/workspaces@2023-09-01' existing = {
  name: workspaceName
}

resource containerApp 'Microsoft.App/containerApps@2024-03-01' existing = {
  name: containerAppName
}

resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: appInsightsName
  location: location
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: workspace.id
    RetentionInDays: 30
  }
  tags: {
    proyecto: 'shiftmanager'
  }
}

var timeRange = '{TimeRange}'

// Consulta KQL sobre Application Insights.
func aiQuery(name string, title string, query string, visualization string, width string) object => {
  type: 3
  name: name
  customWidth: width
  content: {
    version: 'KqlItem/1.0'
    title: title
    query: query
    size: 0
    timeContextFromParameter: 'TimeRange'
    queryType: 0
    resourceType: 'microsoft.insights/components'
    visualization: visualization
  }
}

// Métrica de plataforma de la Container App (Azure Monitor). aggregation: 1 Sum, 3 Max, 4 Average.
func appMetric(name string, title string, appId string, metric string, aggregation int, width string) object => {
  type: 10
  name: name
  customWidth: width
  content: {
    version: 'MetricsItem/2.0'
    title: title
    size: 0
    chartType: 2
    resourceType: 'microsoft.app/containerapps'
    metricScope: 0
    resourceIds: [appId]
    timeContextFromParameter: 'TimeRange'
    metrics: [
      {
        namespace: 'microsoft.app/containerapps'
        metric: 'microsoft.app/containerapps--${metric}'
        aggregation: aggregation
      }
    ]
  }
}

func header(name string, text string) object => {
  type: 1
  name: name
  content: { json: text }
}

var workbookContent = {
  version: 'Notebook/1.0'
  fallbackResourceIds: [appInsights.id]
  items: [
    header('titulo', '# Shiftmanager - Métricas\nTodo junto: peticiones, errores, latencia, base de datos, JVM, pool de conexiones, Container App y logs. Cambiar el rango arriba.')
    {
      type: 9
      name: 'parametros'
      content: {
        version: 'KqlParameterItem/1.0'
        parameters: [
          {
            id: 'time-range'
            name: 'TimeRange'
            label: 'Rango'
            type: 4
            isRequired: true
            value: { durationMs: 3600000 }
            typeSettings: {
              allowCustom: true
              selectableValues: [
                { durationMs: 900000 }
                { durationMs: 3600000 }
                { durationMs: 14400000 }
                { durationMs: 86400000 }
                { durationMs: 604800000 }
              ]
            }
          }
        ]
      }
    }
    aiQuery('kpis', 'Resumen', 'requests\n| where timestamp ${timeRange} and name !has "/actuator/health"\n| summarize Peticiones = count(), Fallidas = countif(success == false), ["Fallidas %"] = round(100.0 * countif(success == false) / count(), 2), ["Promedio ms"] = round(avg(duration), 1), ["P95 ms"] = round(percentile(duration, 95), 1)', 'table', '100')

    header('h-http', '## Peticiones HTTP')
    aiQuery('req-por-codigo', 'Peticiones por código', 'requests\n| where timestamp ${timeRange} and name !has "/actuator/health"\n| summarize count() by resultCode, bin(timestamp, {TimeRange:grain})', 'timechart', '50')
    aiQuery('latencia', 'Latencia (promedio y P95, ms)', 'requests\n| where timestamp ${timeRange} and name !has "/actuator/health"\n| summarize Promedio = avg(duration), P95 = percentile(duration, 95) by bin(timestamp, {TimeRange:grain})', 'timechart', '50')
    aiQuery('endpoints', 'Endpoints', 'requests\n| where timestamp ${timeRange} and name !has "/actuator/health"\n| summarize Peticiones = count(), Fallidas = countif(success == false), ["Promedio ms"] = round(avg(duration), 1), ["P95 ms"] = round(percentile(duration, 95), 1) by Endpoint = name\n| order by Peticiones desc', 'table', '50')
    aiQuery('logins', 'Logins (200 ok / 401 fallido)', 'requests\n| where timestamp ${timeRange} and name has "/api/auth/login"\n| summarize count() by resultCode, bin(timestamp, {TimeRange:grain})', 'barchart', '50')

    header('h-errores', '## Errores')
    aiQuery('excepciones', 'Excepciones', 'exceptions\n| where timestamp ${timeRange}\n| summarize Cantidad = count(), Ultima = max(timestamp) by type, outerMessage\n| order by Cantidad desc', 'table', '50')
    aiQuery('fallidas', 'Peticiones fallidas', 'requests\n| where timestamp ${timeRange} and success == false\n| summarize Cantidad = count() by name, resultCode\n| order by Cantidad desc', 'table', '50')

    header('h-db', '## Base de datos (PostgreSQL)')
    aiQuery('sql', 'Consultas SQL (promedio ms y cantidad)', 'dependencies\n| where timestamp ${timeRange} and type in ("SQL", "postgresql")\n| summarize Cantidad = count(), ["Promedio ms"] = avg(duration) by bin(timestamp, {TimeRange:grain})', 'timechart', '50')
    aiQuery('hikari', 'Pool de conexiones HikariCP', 'customMetrics\n| where timestamp ${timeRange} and name matches regex @"(?i)^hikaricp[._]connections[._]?(active|idle|pending)?$"\n| summarize Valor = avg(value) by name, bin(timestamp, {TimeRange:grain})', 'timechart', '50')

    header('h-jvm', '## JVM')
    aiQuery('heap', 'Memoria JVM (MB)', 'customMetrics\n| where timestamp ${timeRange} and name matches regex @"(?i)^jvm[._]memory[._]used$"\n| summarize MB = sum(value) / 1048576 by bin(timestamp, {TimeRange:grain})', 'timechart', '33')
    aiQuery('cpu', 'CPU del proceso (%)', 'performanceCounters\n| where timestamp ${timeRange} and name == "% Processor Time"\n| summarize CPU = avg(value) by bin(timestamp, {TimeRange:grain})', 'timechart', '33')
    aiQuery('threads', 'Threads y GC', 'customMetrics\n| where timestamp ${timeRange} and name matches regex @"(?i)^jvm[._](threads[._]live|gc[._]pause)$"\n| summarize Valor = avg(value) by name, bin(timestamp, {TimeRange:grain})', 'timechart', '33')

    header('h-aca', '## Container App (Azure Monitor)')
    appMetric('aca-requests', 'Requests', containerApp.id, 'Requests', 1, '33')
    appMetric('aca-replicas', 'Réplicas', containerApp.id, 'Replicas', 3, '33')
    appMetric('aca-restarts', 'Reinicios', containerApp.id, 'RestartCount', 3, '33')
    appMetric('aca-cpu', 'CPU (nanocores)', containerApp.id, 'UsageNanoCores', 4, '50')
    appMetric('aca-mem', 'Memoria (bytes)', containerApp.id, 'WorkingSetBytes', 4, '50')

    header('h-logs', '## Logs')
    aiQuery('trazas', 'Últimos logs WARN/ERROR', 'traces\n| where timestamp ${timeRange} and severityLevel >= 2\n| project timestamp, severityLevel, message\n| order by timestamp desc\n| take 50', 'table', '100')
  ]
  isLocked: false
}

resource workbook 'Microsoft.Insights/workbooks@2023-06-01' = {
  name: guid(resourceGroup().id, 'shiftmanager-workbook')
  location: location
  kind: 'shared'
  properties: {
    displayName: 'Shiftmanager - Métricas'
    category: 'workbook'
    sourceId: appInsights.id
    serializedData: string(workbookContent)
  }
}

output connectionString string = appInsights.properties.ConnectionString
output workbookId string = workbook.id
