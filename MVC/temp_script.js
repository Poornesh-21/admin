// Script to remove all comments from a JavaScript file
const fs = require('fs');
const path = require('path');

// Path to the JavaScript file
const filePath = path.join(__dirname, 'src/main/resources/static/js/admin/completed_services.js');

// Read the file
fs.readFile(filePath, 'utf8', (err, data) => {
  if (err) {
    console.error('Error reading file:', err);
    return;
  }

  // Remove multi-line comments (/**...*/)
  let result = data.replace(/\/\*[\s\S]*?\*\//g, '');
  
  // Remove single-line comments (//)
  result = result.replace(/\/\/.*$/gm, '');
  
  // Remove empty lines
  result = result.replace(/^\s*[\r\n]/gm, '');

  // Write the result back to the file
  fs.writeFile(filePath, result, 'utf8', (err) => {
    if (err) {
      console.error('Error writing file:', err);
      return;
    }
    console.log('All comments have been removed from the file.');
  });
});