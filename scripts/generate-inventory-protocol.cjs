// Generate the limited 26.3 inventory-test adapter from Mojang server data reports.
// Usage: node scripts/generate-inventory-protocol.cjs <26.1.2 reports> <26.3 reports>
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const base = require('minecraft-data')('26.1');
const [baseDirectory, targetDirectory] = process.argv.slice(2);
if (!baseDirectory || !targetDirectory) throw new Error('Supply both server report directories.');
const read = (directory, name) => JSON.parse(fs.readFileSync(path.join(directory, name)));
const oldPackets = read(baseDirectory, 'packets.json');
const newPackets = read(targetDirectory, 'packets.json');
const registry = read(targetDirectory, 'registries.json');
const packetIds = {};
for (const state of ['configuration', 'login', 'play']) {
  packetIds[state] = {};
  for (const [direction, flow] of [['toClient', 'clientbound'], ['toServer', 'serverbound']]) {
    const oldNames = Object.fromEntries(Object.entries(oldPackets[state][flow]).map(([name, value]) => [value.protocol_id, name]));
    const source = base.protocol[state][direction].types.packet[1][0].type[1].mappings;
    const byOfficialName = Object.fromEntries(Object.entries(source).map(([id, name]) => [oldNames[Number(id)], name]));
    packetIds[state][direction] = Object.fromEntries(Object.entries(newPackets[state][flow]).map(([name, value]) => [value.protocol_id, byOfficialName[name] || 'ignored_packet']));
  }
}
const components = Object.fromEntries(Object.entries(registry['minecraft:data_component_type'].entries).map(([name, value]) => [value.protocol_id, name.replace('minecraft:', '')]));
const hashes = {};
for (const [name, directory] of [['base', baseDirectory], ['target', targetDirectory]]) {
  for (const file of ['packets.json', 'registries.json']) hashes[`${name}/${file}`] = crypto.createHash('sha256').update(fs.readFileSync(path.join(directory, file))).digest('hex');
}
const output = { minecraftVersion: '26.3', protocolVersion: 777, baseVersion: '26.1', source: 'net.minecraft.data.Main --reports from Paper 26.1.2-74 and 26.3-227', reportSha256: hashes, packetIds, components };
fs.writeFileSync(path.join(__dirname, 'inventory-protocol-26.3.json'), JSON.stringify(output, null, 2) + '\n');
