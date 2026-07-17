const { Client } = require('pg');
const client = new Client({
  user: 'postgres',
  password: 'password', // Try default passwords or the one from application.yml
  host: 'localhost',
  database: 'domain_system',
  port: 5432,
});

async function run() {
  try {
    await client.connect();
    const res = await client.query("SELECT name, key FROM node_field WHERE name LIKE '%종목%' OR name LIKE '%업종%'");
    console.log(res.rows);
  } catch (err) {
    console.error(err.message);
  } finally {
    await client.end();
  }
}
run();
