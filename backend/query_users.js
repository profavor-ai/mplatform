const { Client } = require('pg');
const client = new Client({ user: 'postgres', password: 'password', host: 'localhost', database: 'domain_system', port: 5432 });
client.connect().then(() => client.query("SELECT id, username FROM users")).then(res => console.log(res.rows)).finally(() => client.end());
