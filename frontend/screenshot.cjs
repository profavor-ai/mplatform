const puppeteer = require('puppeteer');

(async () => {
  try {
    const browser = await puppeteer.launch();
    const page = await browser.newPage();
    await page.setViewport({ width: 1280, height: 800 });

    await page.goto('http://localhost:3002/schema', { waitUntil: 'networkidle2' });

    if (page.url().includes('/login')) {
      const inputs = await page.$$('input');
      if (inputs.length >= 2) {
        await inputs[0].type('admin');
        await inputs[1].type('admin123');
        const submitBtn = await page.$('button[type="submit"]');
        if (submitBtn) {
          await submitBtn.click();
          await page.waitForNavigation({ waitUntil: 'networkidle2' });
        }
      }
    }
    
    // Make sure we are on schema
    if (!page.url().includes('/schema')) {
        await page.goto('http://localhost:3002/schema', { waitUntil: 'networkidle2' });
    }

    await new Promise(r => setTimeout(r, 2000));
    
    // expand all tree nodes if possible
    const expandIcons = await page.$$('.va-tree-node__expand-icon');
    for (const icon of expandIcons) {
      try { await icon.click(); } catch(e){}
    }
    
    await new Promise(r => setTimeout(r, 500));

    // Hover over a leaf node wrapper
    const nodes = await page.$$('.node-wrapper');
    if (nodes.length > 0) {
      await nodes[nodes.length - 1].hover();
      await new Promise(r => setTimeout(r, 500));
    }

    await page.screenshot({ path: 'tree_debug.png' });
    await browser.close();
  } catch (err) {
    console.error(err);
    process.exit(1);
  }
})();
