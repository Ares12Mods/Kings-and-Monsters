import fs from "node:fs";
import path from "node:path";
import zlib from "node:zlib";

function parse(file) {
    let buffer = fs.readFileSync(file);
    if (buffer[0] === 31 && buffer[1] === 139) buffer = zlib.gunzipSync(buffer);
    let offset = 0;
    const byte = () => buffer.readUInt8(offset++);
    const int8 = () => buffer.readInt8(offset++);
    const int16 = () => { const value = buffer.readInt16BE(offset); offset += 2; return value; };
    const uint16 = () => { const value = buffer.readUInt16BE(offset); offset += 2; return value; };
    const int32 = () => { const value = buffer.readInt32BE(offset); offset += 4; return value; };
    const int64 = () => { const value = buffer.readBigInt64BE(offset); offset += 8; return value; };
    const float32 = () => { const value = buffer.readFloatBE(offset); offset += 4; return value; };
    const float64 = () => { const value = buffer.readDoubleBE(offset); offset += 8; return value; };
    const string = () => {
        const length = uint16();
        const value = buffer.toString("utf8", offset, offset + length);
        offset += length;
        return value;
    };
    function tag(type) {
        switch (type) {
            case 1: return { type, value: int8() };
            case 2: return { type, value: int16() };
            case 3: return { type, value: int32() };
            case 4: return { type, value: int64() };
            case 5: return { type, value: float32() };
            case 6: return { type, value: float64() };
            case 7: {
                const length = int32();
                const value = Buffer.from(buffer.subarray(offset, offset + length));
                offset += length;
                return { type, value };
            }
            case 8: return { type, value: string() };
            case 9: {
                const elementType = byte();
                const length = int32();
                const value = [];
                for (let i = 0; i < length; i++) value.push(tag(elementType));
                return { type, elementType, value };
            }
            case 10: {
                const value = {};
                while (true) {
                    const childType = byte();
                    if (childType === 0) break;
                    value[string()] = tag(childType);
                }
                return { type, value };
            }
            case 11: {
                const length = int32();
                const value = [];
                for (let i = 0; i < length; i++) value.push(int32());
                return { type, value };
            }
            case 12: {
                const length = int32();
                const value = [];
                for (let i = 0; i < length; i++) value.push(int64());
                return { type, value };
            }
            default: throw new Error(`Unsupported NBT tag ${type}`);
        }
    }
    const type = byte();
    return { name: string(), root: tag(type) };
}

function encode(document) {
    const chunks = [];
    const push = value => chunks.push(value);
    const number = (method, size, value) => {
        const result = Buffer.alloc(size);
        result[method](value);
        push(result);
    };
    const string = value => {
        const encoded = Buffer.from(value, "utf8");
        number("writeUInt16BE", 2, encoded.length);
        push(encoded);
    };
    function payload(tag) {
        switch (tag.type) {
            case 1: number("writeInt8", 1, tag.value); break;
            case 2: number("writeInt16BE", 2, tag.value); break;
            case 3: number("writeInt32BE", 4, tag.value); break;
            case 4: { const b = Buffer.alloc(8); b.writeBigInt64BE(tag.value); push(b); break; }
            case 5: number("writeFloatBE", 4, tag.value); break;
            case 6: number("writeDoubleBE", 8, tag.value); break;
            case 7: number("writeInt32BE", 4, tag.value.length); push(tag.value); break;
            case 8: string(tag.value); break;
            case 9:
                push(Buffer.from([tag.elementType]));
                number("writeInt32BE", 4, tag.value.length);
                for (const child of tag.value) payload(child);
                break;
            case 10:
                for (const [name, child] of Object.entries(tag.value)) {
                    push(Buffer.from([child.type]));
                    string(name);
                    payload(child);
                }
                push(Buffer.from([0]));
                break;
            case 11:
                number("writeInt32BE", 4, tag.value.length);
                for (const value of tag.value) number("writeInt32BE", 4, value);
                break;
            case 12:
                number("writeInt32BE", 4, tag.value.length);
                for (const value of tag.value) {
                    const b = Buffer.alloc(8);
                    b.writeBigInt64BE(value);
                    push(b);
                }
                break;
            default: throw new Error(`Unsupported NBT tag ${tag.type}`);
        }
    }
    push(Buffer.from([document.root.type]));
    string(document.name);
    payload(document.root);
    return zlib.gzipSync(Buffer.concat(chunks), { level: 9 });
}

const replacements = {
    "minecraft:firefly_bush": "minecraft:short_grass",
    "minecraft:short_dry_grass": "minecraft:dead_bush",
    "minecraft:tall_dry_grass": "minecraft:tall_grass",
    "minecraft:iron_chain": "minecraft:chain"
};

function convert(input, output) {
    const document = parse(input);
    const root = document.root.value;
    root.DataVersion = { type: 3, value: 3955 };
    for (const paletteEntry of root.palette.value) {
        const entry = paletteEntry.value;
        if (entry.id) {
            entry.Name = { type: 8, value: replacements[entry.id.value] ?? entry.id.value };
            delete entry.id;
        }
        if (entry.properties) {
            entry.Properties = entry.properties;
            delete entry.properties;
        }
    }
    for (const block of root.blocks.value) {
        const nbt = block.value.nbt?.value;
        if (!nbt) continue;
        if (nbt.sherds?.type === 10) {
            const sides = ["back", "left", "right", "front"];
            const items = sides.map(side => nbt.sherds.value[side]?.value.id?.value ?? "minecraft:brick");
            if (items.some(item => item !== "minecraft:brick")) {
                nbt.sherds = { type: 9, elementType: 8, value: items.map(value => ({ type: 8, value })) };
            } else {
                delete nbt.sherds;
            }
        }
        if (nbt.id?.value === "minecraft:command_block" && nbt.LastOutput?.type === 10) {
            delete nbt.LastOutput;
        }
    }
    fs.mkdirSync(path.dirname(output), { recursive: true });
    fs.writeFileSync(output, encode(document));
    const palette = root.palette.value.map(tag => tag.value.Name?.value ?? tag.value.id?.value ?? "unknown");
    const solidBlocksByY = {};
    const containers = [];
    const fences = [];
    for (const block of root.blocks.value) {
        const value = block.value;
        const position = value.pos.value.map(tag => tag.value);
        const blockName = palette[value.state.value];
        if (blockName !== "minecraft:air" && blockName !== "minecraft:structure_void") {
            solidBlocksByY[position[1]] = (solidBlocksByY[position[1]] ?? 0) + 1;
        }
        if (blockName === "minecraft:chest" || blockName === "minecraft:barrel"
                || blockName === "minecraft:trapped_chest") {
            containers.push({ block: blockName, pos: position });
        }
        if (blockName.endsWith("_fence") || blockName.endsWith("_fence_gate")) {
            fences.push({ block: blockName, pos: position });
        }
    }
    return {
        name: path.basename(output),
        size: root.size.value.map(tag => tag.value),
        blocks: root.blocks.value.length,
        palette: root.palette.value.length,
        solidBlocksByY,
        containers,
        fences
    };
}

const [outputDirectory, worldDirectory, ...pairs] = process.argv.slice(2);
if (!outputDirectory || !worldDirectory || pairs.length === 0 || pairs.length % 2 !== 0) {
    throw new Error("Usage: node convert_structures_to_1_21_1.mjs <output-dir> <world-dir> <input> <output-name> [...]");
}
fs.mkdirSync(outputDirectory, { recursive: true });
fs.mkdirSync(worldDirectory, { recursive: true });
for (let i = 0; i < pairs.length; i += 2) {
    const info = convert(pairs[i], path.join(outputDirectory, pairs[i + 1]));
    fs.copyFileSync(path.join(outputDirectory, pairs[i + 1]), path.join(worldDirectory, pairs[i + 1]));
    console.log(JSON.stringify(info));
}
