 
(async () => {
  try {
    const res = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'admin1234' })
    });
    const data = await res.json();
    
    const createRes = await fetch('http://localhost:8080/api/nodes/935102dc-bccf-47e0-8d65-0d883186472f/records', {
      method: 'POST',
      headers: { 
        Authorization: 'Bearer ' + data.token,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        data: "{\"test\":\"data\"}",
        requesterId: "123e4567-e89b-12d3-a456-426614174000"
      })
    });
    
    console.log('Status:', createRes.status);
    console.log('Body:', await createRes.text());
  } catch(e) { console.error(e) }
})();
