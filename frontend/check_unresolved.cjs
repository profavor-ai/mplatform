const puppeteer = require('puppeteer');
(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:3002/schema', { waitUntil: 'networkidle2' });
  if (page.url().includes('/login')) {
    const inputs = await page.$$('input');
    await inputs[0].type('admin');
    await inputs[1].type('admin123');
    await page.click('button[type="submit"]');
    await page.waitForNavigation({ waitUntil: 'networkidle2' });
  }
  if (!page.url().includes('/schema')) {
    await page.goto('http://localhost:3002/schema', { waitUntil: 'networkidle2' });
  }
  const html = await page.evaluate(() => document.body.innerHTML);
  console.log(html.includes('schematreenode') ? 'UNRESOLVED_COMPONENT' : 'OTHER');
  await browser.close();
})();
