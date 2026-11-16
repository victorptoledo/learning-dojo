from machine import Instruction, decode, disassemble, run


def test_decode_an_addi_with_a_negative_immediate():
    # 0110 001 000 111101: addi r1, r0, -3
    assert decode(0x623D) == Instruction("addi", rd=1, rs=0, imm=-3)
    assert disassemble(0x623D) == "addi r1, r0, -3"


def test_run_a_tiny_program():
    # li r1, 5 / addi r2, r1, -3 / halt
    result = run([0x7205, 0x647D, 0x0000])
    assert result.registers[1:3] == [5, 2]
    assert result.instructions == 3 and result.cycles == 3
