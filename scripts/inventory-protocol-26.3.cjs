// Limited native-wire adapter for the inventory checks, not a general game client.
const base = require('minecraft-data')('26.1');
const definition = require('./inventory-protocol-26.3.json');
const assert = require('node:assert/strict');
const mc = require('minecraft-protocol');

// Decode only packets used by login/configuration and these inventory tests.
// Consume unrelated world/entity/rendering packets as opaque payloads.
const decoded = new Set([
  'disconnect', 'encryption_begin', 'success', 'compress', 'login_plugin_request', 'cookie_request',
  'custom_payload', 'finish_configuration', 'keep_alive', 'ping', 'select_known_packs', 'code_of_conduct',
  'login', 'position', 'open_window', 'window_items', 'set_slot', 'close_window', 'kick_disconnect',
  'start_configuration', 'chunk_batch_finished'
]);
const protocol = { types: { SlotComponentType: ['mapper', { type: 'varint', mappings: definition.components }] } };
for (const state of ['configuration', 'login', 'play']) {
  protocol[state] = {};
  for (const direction of ['toClient', 'toServer']) {
    const packet = structuredClone(base.protocol[state][direction].types.packet);
    const mappings = Object.fromEntries(Object.entries(definition.packetIds[state][direction]).map(([id, name]) => [`0x${Number(id).toString(16).padStart(2, '0')}`, name]));
    if (direction === 'toClient') {
      for (const id of Object.keys(mappings)) if (!decoded.has(mappings[id])) mappings[id] = 'ignored_packet';
      packet[1][1].type[1].fields.ignored_packet = 'restBuffer';
    }
    packet[1][0].type[1].mappings = mappings;
    protocol[state][direction] = { types: { packet } };
  }
}
// Wire fields verified against the 26.3 server's STREAM_CODEC implementations:
// ClientboundLoginFinishedPacket adds a session UUID, ClientboundLoginPacket
// adds onlineMode, and ServerboundAcceptTeleportationPacket adds position/rotation.
protocol.login.toClient.types.packet_success = structuredClone(base.protocol.login.toClient.types.packet_success);
protocol.login.toClient.types.packet_success[1].push({ name: 'sessionId', type: 'UUID' });
protocol.play.toClient.types.packet_login = structuredClone(base.protocol.play.toClient.types.packet_login);
protocol.play.toClient.types.packet_login[1].splice(-1, 0, { name: 'onlineMode', type: 'bool' });
protocol.play.toServer.types.packet_teleport_confirm = ['container', [
  { name: 'teleportId', type: 'varint' },
  { name: 'x', type: 'f64' }, { name: 'y', type: 'f64' }, { name: 'z', type: 'f64' },
  { name: 'yaw', type: 'f32' }, { name: 'pitch', type: 'f32' }
]];
class InventoryClient extends mc.Client {
  write(name, data) {
    if (name === 'set_protocol') data = { ...data, protocolVersion: definition.protocolVersion };
    return super.write(name, data);
  }
}
module.exports = {
  options: { version: definition.baseVersion, customPackets: { [definition.baseVersion]: protocol }, Client: InventoryClient },
  verifyServer(status) {
    assert.equal(status.version.protocol, definition.protocolVersion, 'Server must speak native 26.3 protocol 777');
    assert.match(status.version.name, /26\.3/, 'Expected a 26.3 server');
  }
};
