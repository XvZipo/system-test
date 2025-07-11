// SPDX-License-Identifier: GPL-3.0
pragma solidity >=0.0.0;

contract C {
    function copyBytes() external returns (bytes memory)
    {
        bytes memory ret = "aaaaa";
        return ret;
    }

    function copyString() external returns (string memory)
    {
        return "return string";
    }
}
