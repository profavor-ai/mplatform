 

(async () => {
  try {
    console.log('Testing record creation API...');
    const res = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'admin1234' })
    });
    
    // We expect this to fail or succeed, but wait...
    // I can just hit the api directly with a dummy token since there's no real jwt validation in my local mock?
    // Wait, the backend uses JWT. Let's see if we can get a token.
    if (!res.ok) {
       console.log('Login failed', res.status);
    }
    const data = await res.json();
    console.log('Token:', data.token);

    // Get domains
    const domainRes = await fetch('http://localhost:8080/api/domains', {
      headers: { Authorization: `Bearer ${data.token}` }
    });
    const domains = await domainRes.json();
    if (domains.length === 0) {
      console.log('No domains found');
      return;
    }
    
    // Get nodes
    const nodeRes = await fetch(`http://localhost:8080/api/domains/${domains[0].id}/nodes/tree`, {
      headers: { Authorization: `Bearer ${data.token}` }
    });
    const nodes = await nodeRes.json();
    if (nodes.length === 0) {
      console.log('No nodes found');
      return;
    }
    
    // Create record
    const createRes = await fetch(`http://localhost:8080/api/nodes/${nodes[0].id}/records`, {
      method: 'POST',
      headers: { 
        Authorization: `Bearer ${data.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        data: "{\"test\":\"data\"}",
        requesterId: "123e4567-e89b-12d3-a456-426614174000"
      })
    });
    
    if (createRes.ok) {
      console.log('SUCCESS:', await createRes.json());
    } else {
      console.log('FAILED:', createRes.status);
      console.log('ERROR:', await createRes.text());
    }
    
  } catch(e) {
    console.error(e);
  }
})();
