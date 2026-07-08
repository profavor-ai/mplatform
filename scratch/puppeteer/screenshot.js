const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  
  // Emulate a mobile device (iPhone X)
  await page.setViewport({
    width: 375,
    height: 812,
    isMobile: true,
    hasTouch: true
  });
  
  // Navigate to records page
  console.log("Navigating...");
  await page.goto('http://localhost:3000/records', { waitUntil: 'networkidle2' });
  
  // Wait a bit just in case
  await new Promise(r => setTimeout(r, 2000));
  
  // Take screenshot
  const outPath = 'C:\\Users\\Profavor\\.gemini\\antigravity\\brain\\67feb8fe-67ea-4880-97a2-df1c011fef29\\mobile_screenshot.png';
  await page.screenshot({ path: outPath, fullPage: false });
  console.log('Screenshot saved to ' + outPath);
  
  await browser.close();
})();
