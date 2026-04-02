contract getAddressChange {
    constructor() public payable {}
    function testaddress1() public view returns(address) {
        return this.getamount.address;  //0.6.0

    }
    function getamount(address) external view returns(uint256) {
        return address(this).balance;
    }
}