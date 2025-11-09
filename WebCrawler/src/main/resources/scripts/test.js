/**
 * @param {Shv} shv
 */
function run(shv) {
  console.log('injected', shv);
  return shv.findOne('head title')?.textContent;
}
