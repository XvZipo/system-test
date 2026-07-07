// SPDX-License-Identifier: GPL-3.0
pragma solidity >=0.8.28;

contract Solc0828DailyChainProbe {
  constructor() payable {}

  function f() public view returns (uint256) {
    return getchainparameter(0);
  }

  function param(uint64 i) public view returns (uint256) {
    return getchainparameter(i);
  }
}
