//pragma solidity ^0.4.24;

 contract token{
     constructor() public payable {}

     function failTransferTokenRevert(address payable toAddress,uint256 amount, trcToken id) public payable{
         toAddress.transferToken(amount,id);
         require(1==2);
     }

     function failTransferTokenError(address payable toAddress,uint256 amount, trcToken id) public payable{
         toAddress.transferToken(amount,id);
         assert(1==2);
     }

 }
 contract B{
    uint256 public flag = 0;
    constructor() public payable {}
    function() external payable {}
}