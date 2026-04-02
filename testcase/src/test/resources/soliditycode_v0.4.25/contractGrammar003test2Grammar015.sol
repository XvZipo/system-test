pragma solidity ^0.4.0;

contract ExecuteFallback{

  event FallbackCalled(bytes data);
  function(){
    FallbackCalled(msg.data);
  }

  event ExistFuncCalled(bytes data, uint256 para);
  function existFunc(uint256 para){
    ExistFuncCalled(msg.data, para);
  }

  function callExistFunc(){
    bytes4 funcIdentifier = bytes4(keccak256("existFunc(uint256)"));
    this.call(funcIdentifier, uint256(1));
  }

  function callNonExistFunc(){
    bytes4 funcIdentifier = bytes4(keccak256("functionNotExist()"));
    this.call(funcIdentifier);
  }

  function ExistFuncCalledTopic() view returns(bytes32){
      return keccak256("ExistFuncCalled(bytes,uint256)");
  }
    function FallbackCalledTopic() view returns(bytes32){
      return keccak256("FallbackCalled(bytes)");
  }
}