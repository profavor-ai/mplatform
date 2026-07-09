const XLSX = require('xlsx');

const ws = XLSX.utils.aoa_to_sheet([
  ["Name", "Type"]
]);

ws['!dataValidation'] = [
  {
    sqref: 'B2:B100',
    type: 'list',
    formula1: '"Option1,Option2"'
  }
];

const wb = XLSX.utils.book_new();
XLSX.utils.book_append_sheet(wb, ws, "Sheet1");
XLSX.writeFile(wb, "test.xlsx");
