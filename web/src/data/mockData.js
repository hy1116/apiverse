export const MOCK_USER = {
  id: 1,
  email: 'dev@hypepia.com',
  companyName: 'Hypepia Inc.',
  tier: 'FREE',
  token: 'mock-jwt-token',
}

export const mockApiProducts = [
  {
    id: 1,
    name: '기상청 날씨 API',
    description: '실시간 기온, 강수량, 바람, 습도 등 전국 기상 관측 데이터를 제공하는 공공 API입니다.',
    baseUrl: 'https://api.weather.go.kr/v1',
    isPremium: false,
    isActive: true,
    category: 'Weather',
    callsPerSec: 5,
    spec: {
      openapi: '3.0.0',
      info: { title: '기상청 날씨 API', version: '1.0.0', description: '실시간 기상 데이터 조회' },
      servers: [{ url: 'https://api.weather.go.kr/v1' }],
      paths: {
        '/current': {
          get: {
            summary: '현재 날씨 조회',
            parameters: [
              { name: 'region', in: 'query', required: true, schema: { type: 'string' }, description: '지역명 (예: 서울)' },
            ],
            responses: {
              200: {
                description: '성공',
                content: {
                  'application/json': {
                    example: { region: '서울', temp: 22.4, humidity: 65, wind: 3.2, condition: '맑음' },
                  },
                },
              },
            },
            security: [{ ApiKeyAuth: [] }],
          },
        },
        '/forecast': {
          get: {
            summary: '7일 날씨 예보',
            parameters: [
              { name: 'region', in: 'query', required: true, schema: { type: 'string' } },
            ],
            responses: { 200: { description: '성공' } },
            security: [{ ApiKeyAuth: [] }],
          },
        },
      },
      components: {
        securitySchemes: { ApiKeyAuth: { type: 'apiKey', in: 'header', name: 'X-API-KEY' } },
      },
    },
  },
  {
    id: 2,
    name: '공공 주소 검색 API',
    description: '도로명 주소, 지번 주소, 우편번호 등 전국 주소 데이터를 검색할 수 있는 API입니다.',
    baseUrl: 'https://api.juso.go.kr/v2',
    isPremium: false,
    isActive: true,
    category: 'Location',
    callsPerSec: 10,
    spec: {
      openapi: '3.0.0',
      info: { title: '공공 주소 검색 API', version: '2.0.0' },
      servers: [{ url: 'https://api.juso.go.kr/v2' }],
      paths: {
        '/search': {
          get: {
            summary: '주소 검색',
            parameters: [
              { name: 'keyword', in: 'query', required: true, schema: { type: 'string' } },
              { name: 'page', in: 'query', schema: { type: 'integer', default: 1 } },
              { name: 'size', in: 'query', schema: { type: 'integer', default: 10 } },
            ],
            responses: { 200: { description: '성공' } },
            security: [{ ApiKeyAuth: [] }],
          },
        },
      },
      components: {
        securitySchemes: { ApiKeyAuth: { type: 'apiKey', in: 'header', name: 'X-API-KEY' } },
      },
    },
  },
  {
    id: 3,
    name: '한국관광공사 관광정보 API',
    description: '전국 관광지, 숙박, 음식점, 축제 등 관광 정보를 제공하는 공공 API입니다.',
    baseUrl: 'https://api.visitkorea.or.kr/v1',
    isPremium: false,
    isActive: true,
    category: 'Tourism',
    callsPerSec: 5,
    spec: {
      openapi: '3.0.0',
      info: { title: '한국관광공사 관광정보 API', version: '1.0.0' },
      servers: [{ url: 'https://api.visitkorea.or.kr/v1' }],
      paths: {
        '/attractions': {
          get: {
            summary: '관광지 목록 조회',
            parameters: [
              { name: 'area', in: 'query', schema: { type: 'string' } },
              { name: 'category', in: 'query', schema: { type: 'string', enum: ['관광지', '숙박', '음식점', '축제'] } },
            ],
            responses: { 200: { description: '성공' } },
            security: [{ ApiKeyAuth: [] }],
          },
        },
      },
      components: {
        securitySchemes: { ApiKeyAuth: { type: 'apiKey', in: 'header', name: 'X-API-KEY' } },
      },
    },
  },
  {
    id: 4,
    name: '실시간 주식 시세 API',
    description: '한국거래소(KRX) 실시간 주가, 거래량, 시가총액 데이터를 제공합니다. Pro 플랜 이상 사용 가능.',
    baseUrl: 'https://api.krx-data.co.kr/v1',
    isPremium: true,
    isActive: true,
    category: 'Finance',
    callsPerSec: 20,
    spec: {
      openapi: '3.0.0',
      info: { title: '실시간 주식 시세 API', version: '1.0.0' },
      servers: [{ url: 'https://api.krx-data.co.kr/v1' }],
      paths: {
        '/quote/{ticker}': {
          get: {
            summary: '종목 현재가 조회',
            parameters: [
              { name: 'ticker', in: 'path', required: true, schema: { type: 'string' }, description: '종목 코드 (예: 005930)' },
            ],
            responses: { 200: { description: '성공' } },
            security: [{ ApiKeyAuth: [] }],
          },
        },
      },
      components: {
        securitySchemes: { ApiKeyAuth: { type: 'apiKey', in: 'header', name: 'X-API-KEY' } },
      },
    },
  },
]

export const mockApiKeys = [
  {
    id: 1,
    apiKeyValue: 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6',
    apiProductId: 1,
    apiProductName: '기상청 날씨 API',
    monthlyQuota: -1,
    usedQuota: 127,
    isActive: true,
    createdAt: '2026-06-01T09:00:00',
  },
  {
    id: 2,
    apiKeyValue: 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4',
    apiProductId: 2,
    apiProductName: '공공 주소 검색 API',
    monthlyQuota: -1,
    usedQuota: 43,
    isActive: true,
    createdAt: '2026-06-10T14:30:00',
  },
]

export const mockUsageData = [
  { date: '06/24', requests: 45, errors: 2 },
  { date: '06/25', requests: 92, errors: 5 },
  { date: '06/26', requests: 78, errors: 1 },
  { date: '06/27', requests: 134, errors: 8 },
  { date: '06/28', requests: 61, errors: 0 },
  { date: '06/29', requests: 110, errors: 3 },
  { date: '06/30', requests: 88, errors: 2 },
]

