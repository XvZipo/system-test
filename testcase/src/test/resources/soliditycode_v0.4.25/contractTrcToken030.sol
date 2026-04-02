pragma solidity ^0.4.24;

 contract token{
     constructor() public payable{}

     function kill(address toAddress) payable public{
         selfdestruct(toAddress);
     }

 }

contract B{
    constructor() public payable {}
    function() public payable {}
}