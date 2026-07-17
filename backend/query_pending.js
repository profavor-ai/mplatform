const http = require('http');

const options = {
  hostname: 'localhost',
  port: 8080,
  path: '/api/approval-requests/todos?assigneeId=41882f81-4d84-4480-9d3b-6c896283c6d9&page=0&size=100',
  method: 'GET',
};

const req = http.request(options, (res) => {
  let data = '';
  res.on('data', (chunk) => {
    data += chunk;
  });
  res.on('end', () => {
    console.log("API Response:", data);
  });
});

req.on('error', (e) => {
  console.error(e);
});

req.end();
