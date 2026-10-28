// Run with NODE_PATH pointing to a directory containing minecraft-protocol.
// JAVA_HOME, PAPER_SERVER_JAR and PAPER_CACHE_DIRECTORY select the local fixture.
// Set PROTOCOLLIB_JAR to exercise the overlay; omit it to exercise the fallback.
const fs = require('node:fs');
const path = require('node:path');
const net = require('node:net');
const { spawn } = require('node:child_process');
const mc = require('minecraft-protocol');
const assert = require('node:assert/strict');
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
const root = path.resolve(__dirname, '..');
const overlay = Boolean(process.env.PROTOCOLLIB_JAR);
const version = process.argv[2] || '1.21.11';
const adapter = version === '26.3' ? require('./inventory-protocol-26.3.cjs') : null;
const work = path.join(root, 'target', 'client-checks', `${version}-${overlay ? 'overlay' : 'fallback'}-${Date.now()}`);
let server, client, clientError, log = '', packets = [];
const deadline = setTimeout(() => { console.error('Client check timed out'); server?.kill(); process.exit(1); }, 180000);
async function until(check, message, timeout = 30000) {
  const end = Date.now() + timeout;
  while (Date.now() < end) { if (clientError) throw clientError; if (check()) return; await delay(50); }
  throw new Error(message);
}
async function run() {
  fs.mkdirSync(path.join(work, 'plugins'), { recursive: true });
  for (const dir of ['cache', 'libraries', 'versions']) {
    fs.cpSync(path.join(process.env.PAPER_CACHE_DIRECTORY, dir), path.join(work, dir), { recursive: true });
  }
  fs.copyFileSync(path.join(root, 'target/InvUnload.jar'), path.join(work, 'plugins/InvUnload.jar'));
  if (overlay) fs.copyFileSync(process.env.PROTOCOLLIB_JAR, path.join(work, 'plugins/ProtocolLib.jar'));
  const socket = net.createServer();
  await new Promise(resolve => socket.listen(0, '127.0.0.1', resolve));
  const port = socket.address().port;
  await new Promise(resolve => socket.close(resolve));
  fs.writeFileSync(path.join(work, 'eula.txt'), 'eula=true\n');
  fs.writeFileSync(path.join(work, 'server.properties'), `online-mode=false\nserver-ip=127.0.0.1\nserver-port=${port}\nenforce-secure-profile=false\nwhite-list=false\nenforce-whitelist=false\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\n`);
  server = spawn(path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java'), ['-Xms256M', '-Xmx1G', '-Dterminal.jline=false', '-jar', path.resolve(process.env.PAPER_SERVER_JAR), '--nogui'], { cwd: work, windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'] });
  const appendLog = data => { log += data.toString().replace(/\x1b\[[0-9;]*m/g, ''); };
  server.stdout.on('data', appendLog);
  server.stderr.on('data', appendLog);
  await until(() => log.includes('Done ('), 'Server did not start', 120000);
  if (overlay) assert(log.includes('Hooked into ProtocolLib for ignored slot previews.'), 'Overlay hook did not initialize');
  const consoleCommand = text => server.stdin.write(text + '\n');
  if (adapter) {
    const status = await new Promise((resolve, reject) => mc.ping({ host: '127.0.0.1', port }, (error, result) => error ? reject(error) : resolve(result)));
    adapter.verifyServer(status);
    console.log(`Testing native ${status.version.name}, protocol ${status.version.protocol}`);
  }
  client = mc.createClient({ host: '127.0.0.1', port, username: 'InvUnloadTest', auth: 'offline', version, ...adapter?.options });
  let joined = false;
  client.on('error', error => { clientError = error; });
  client.on('disconnect', data => { clientError = new Error(`Disconnected: ${JSON.stringify(data)}`); });
  client.on('kick_disconnect', data => { clientError = new Error(`Kicked: ${JSON.stringify(data)}`); });
  client.on('packet', (data, meta, buffer, fullBuffer) => {
    if (meta.size !== fullBuffer.length) clientError = new Error(`Incomplete ${meta.name} packet: ${meta.size}/${fullBuffer.length} bytes`);
    packets.push({ name: meta.name, data });
    if (meta.name === 'login') joined = true;
    if (meta.name === 'position') client.write('teleport_confirm', { teleportId: data.teleportId, x: data.x, y: data.y, z: data.z, yaw: data.yaw, pitch: data.pitch });
  });
  await until(() => joined || clientError, 'Client did not join');
  if (clientError) throw clientError;
  consoleCommand('op InvUnloadTest');
  consoleCommand('gamemode creative InvUnloadTest');
  consoleCommand('item replace entity InvUnloadTest hotbar.0 with minecraft:diamond 7');
  await delay(500);
  packets = [];
  client.write('chat_command', { command: 'unload slots' });
  await until(() => packets.some(p => p.name === 'open_window'), 'Menu did not open');
  const window = packets.find(p => p.name === 'open_window').data.windowId;
  await until(() => packets.some(p => p.name === 'window_items' && p.data.windowId === window), 'Menu contents missing');
  await delay(300);
  const items = packets.find(p => p.name === 'window_items' && p.data.windowId === window).data.items;
  assert.equal(items.length, overlay ? 45 : 81, 'Incorrect overlay/fallback menu size');
  const previewSlot = 36; // Overlay hotbar or fallback hotbar preview.
  packets = [];
  client.write('window_click', { windowId: window, stateId: 0, slot: previewSlot, mouseButton: 0, mode: 0, changedSlots: [], cursorItem: null });
  const playerDirectory = path.join(work, 'plugins/InvUnload/playerdata');
  await until(() => fs.existsSync(playerDirectory) && fs.readdirSync(playerDirectory).some(f => f.endsWith('.yml')), 'Click did not persist settings');
  const playerFile = path.join(playerDirectory, fs.readdirSync(playerDirectory).find(f => f.endsWith('.yml')));
  await until(() => /lockedSlots:\s*\n- 0/.test(fs.readFileSync(playerFile, 'utf8')), 'Hotbar click did not lock slot 0');
  await delay(300);
  const serialized = JSON.stringify(packets);
  assert(serialized.includes('enchantment_glint_override'), 'Client did not receive locked-item glint');
  client.write('close_window', { windowId: window });
  await delay(300);
  consoleCommand('data get entity InvUnloadTest Inventory');
  await until(() => /minecraft:diamond/.test(log) && /count: 7/.test(log), 'Actual diamond stack was not preserved');
  const realInventory = log.split('\n').find(line => line.includes('following entity data:'));
  assert(!realInventory.includes('enchantment_glint_override'), 'Preview metadata leaked into the real stack');
  client.write('chat_command', { command: 'unload slots' });
  await delay(500);
  assert(/lockedSlots:\s*\n- 0/.test(fs.readFileSync(playerFile, 'utf8')), 'Lock was lost when reopening');
  if (clientError) throw clientError;
  assert(!/Could not (?:send|rewrite) ignored slot preview packet|generated an exception|Error occurred while enabling InvUnload/.test(log), 'Plugin error in server log');
  fs.writeFileSync(path.join(work, 'client-result.txt'), 'PASS\nMenu open, slot click, persistence, client glint, inventory preservation, close/reopen.\n');
  console.log(`PASS ${version} ${overlay ? 'ProtocolLib overlay' : 'fallback'}: ${work}`);
}
run().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => {
  client?.end();
  if (server) {
    server.stdin.write('stop\n');
    await Promise.race([new Promise(resolve => server.once('exit', resolve)), delay(15000)]);
    if (server.exitCode === null) server.kill();
  }
  fs.writeFileSync(path.join(work, 'console.log'), log);
  clearTimeout(deadline);
});
