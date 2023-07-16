USE [OnlineShop]
go

IF OBJECT_ID('Line', 'U') IS NOT NULL
DROP TABLE [Line]
go

IF OBJECT_ID('Transaction', 'U') IS NOT NULL
DROP TABLE [Transaction]
go

IF OBJECT_ID('OrderItem', 'U') IS NOT NULL
DROP TABLE [OrderItem]
go

IF OBJECT_ID('Article', 'U') IS NOT NULL
DROP TABLE [Article]
go

IF OBJECT_ID('Shop', 'U') IS NOT NULL
DROP TABLE [Shop]
go

IF OBJECT_ID('Order', 'U') IS NOT NULL
DROP TABLE [Order]
go

IF OBJECT_ID('Buyer', 'U') IS NOT NULL
DROP TABLE [Buyer]
go

IF OBJECT_ID('City', 'U') IS NOT NULL
DROP TABLE [City]
go

CREATE TABLE [City]
( 
	[Id]                 integer  IDENTITY  NOT NULL ,
	[Name]               varchar(100)  NOT NULL 
)
go

CREATE TABLE [Buyer]
( 
	[Id]                 integer  IDENTITY  NOT NULL ,
	[Name]               varchar(100)  NOT NULL ,
	[IdCity]             integer  NOT NULL ,
	[Balance]            decimal(10,3)  NOT NULL 
)
go

CREATE TABLE [Line]
( 
	[Distance]           integer  NOT NULL 
	CONSTRAINT [POSITIVE_VALUE_CHECK_1816472920]
		CHECK  ( Distance >= 0 ),
	[Id1]                integer  NOT NULL ,
	[Id2]                integer  NOT NULL 
)
go

CREATE TABLE [Order]
( 
	[Id]                 integer  IDENTITY  NOT NULL ,
	[Status]             varchar(100)  NOT NULL 
	CONSTRAINT [ALLOWED_STATUS_223283311]
		CHECK  ( [Status]='created' OR [Status]='sent' OR [Status]='arrived' ),
	[TotalPrice]         decimal(10,3)  NULL 
	CONSTRAINT [POSITIVE_VALUE_CHECK_301587470]
		CHECK  ( TotalPrice >= 0 ),
	[IdBuyer]         integer  NOT NULL ,
	[BuyerDiscount]   integer  NOT NULL
	CONSTRAINT [DEFAULT_ZERO_855831185]
		 DEFAULT  0
	CONSTRAINT [POSITIVE_VALUE_CHECK_2057285097]
		CHECK  ( BuyerDiscount >= 0 ),
	[DiscountedPrice]    decimal(10,3)  NULL ,
	[DateNearest]        datetime  NULL ,
	[DateArrived]        datetime  NULL ,
	[IdNearestCity]      integer  NULL ,
	[DateSent]           datetime  NULL ,
	[DaysToAssemble]     integer  NULL 
)
go

CREATE TABLE [OrderItem]
( 
	[Id]                 integer  IDENTITY  NOT NULL ,
	[IdOrder]            integer  NOT NULL ,
	[Quantity]           integer  NOT NULL 
	CONSTRAINT [DEFAULT_ONE_1217751168]
		 DEFAULT  1
	CONSTRAINT [POSITIVE_VALUE_CHECK_590425728]
		CHECK  ( Quantity >= 0 ),
	[IdArticle]          integer  NOT NULL
)
go

CREATE TABLE [Article]
( 
	[Id]                 integer  IDENTITY  NOT NULL ,
	[Price]              decimal(10,3)  NOT NULL 
	CONSTRAINT [POSITIVE_VALUE_CHECK_642198397]
		CHECK  ( Price >= 0 ),
	[Quantity]           integer  NOT NULL 
	CONSTRAINT [POSITIVE_VALUE_CHECK_870434508]
		CHECK  ( Quantity >= 0 ),
	[IdShop]            integer  NOT NULL ,
	[Name]               varchar(100)  NOT NULL 
)
go

CREATE TABLE [Shop]
( 
	[Id]                 integer  IDENTITY  NOT NULL ,
	[Name]               varchar(100)  NOT NULL ,
	[Discount]           decimal(10,3)  NOT NULL 
	CONSTRAINT [DEFAULT_ZERO_472203648]
		 DEFAULT  0
	CONSTRAINT [POSITIVE_VALUE_CHECK_909647367]
		CHECK  ( Discount >= 0 ),
	[IdCity]             integer  NOT NULL ,
	[Balance]            decimal(10,3)  NOT NULL 
)
go

CREATE TABLE [Transaction]
( 
	[Id]                 integer  IDENTITY  NOT NULL ,
	[Amount]             decimal(10,3)  NOT NULL 
	CONSTRAINT [POSITIVE_VALUE_CHECK_1411522684]
		CHECK  ( Amount >= 0 ),
	[IdOrder]            integer  NOT NULL ,
	[IdShop]            integer  NULL ,
	[Date]               datetime  NOT NULL ,
	[IdBuyer]         integer  NULL
)
go

ALTER TABLE [City]
	ADD CONSTRAINT [XPKCity] PRIMARY KEY  CLUSTERED ([Id] ASC)
go

ALTER TABLE [City]
	ADD CONSTRAINT [XAK1City] UNIQUE ([Name]  ASC)
go

ALTER TABLE [Buyer]
	ADD CONSTRAINT [XPKBuyer] PRIMARY KEY  CLUSTERED ([Id] ASC)
go

ALTER TABLE [Line]
	ADD CONSTRAINT [XPKLine] PRIMARY KEY  CLUSTERED ([Id1] ASC,[Id2] ASC)
go

ALTER TABLE [Order]
	ADD CONSTRAINT [XPKOrder] PRIMARY KEY  CLUSTERED ([Id] ASC)
go

ALTER TABLE [OrderItem]
	ADD CONSTRAINT [XPKOrderItem] PRIMARY KEY  CLUSTERED ([Id] ASC)
go

ALTER TABLE [Article]
	ADD CONSTRAINT [XPKArticle] PRIMARY KEY  CLUSTERED ([Id] ASC)
go

ALTER TABLE [Shop]
	ADD CONSTRAINT [XPKShop] PRIMARY KEY  CLUSTERED ([Id] ASC)
go

ALTER TABLE [Shop]
	ADD CONSTRAINT [XAK1Shop] UNIQUE ([Name]  ASC)
go

ALTER TABLE [Transaction]
	ADD CONSTRAINT [XPKTransaction] PRIMARY KEY  CLUSTERED ([Id] ASC)
go


ALTER TABLE [Buyer]
	ADD CONSTRAINT [R_9] FOREIGN KEY ([IdCity]) REFERENCES [City]([Id])
		ON DELETE NO ACTION
		ON UPDATE NO ACTION
go


ALTER TABLE [Line]
	ADD CONSTRAINT [R_10] FOREIGN KEY ([Id1]) REFERENCES [City]([Id])
		ON DELETE NO ACTION
		ON UPDATE NO ACTION
go

ALTER TABLE [Line]
	ADD CONSTRAINT [R_12] FOREIGN KEY ([Id2]) REFERENCES [City]([Id])
		ON DELETE NO ACTION
		ON UPDATE NO ACTION
go


ALTER TABLE [Order]
	ADD CONSTRAINT [R_3] FOREIGN KEY ([IdBuyer]) REFERENCES [Buyer]([Id])
		ON DELETE NO ACTION
		ON UPDATE NO ACTION
go

ALTER TABLE [Order]
	ADD CONSTRAINT [R_15] FOREIGN KEY ([IdNearestCity]) REFERENCES [City]([Id])
		ON DELETE NO ACTION
		ON UPDATE NO ACTION
go


ALTER TABLE [OrderItem]
	ADD CONSTRAINT [R_4] FOREIGN KEY ([IdOrder]) REFERENCES [Order]([Id])
		ON DELETE NO ACTION
		ON UPDATE NO ACTION
go

ALTER TABLE [OrderItem]
	ADD CONSTRAINT [R_8] FOREIGN KEY ([IdArticle]) REFERENCES [Article]([Id])
		ON DELETE NO ACTION
		ON UPDATE NO ACTION
go


ALTER TABLE [Article]
	ADD CONSTRAINT [R_2] FOREIGN KEY ([IdShop]) REFERENCES [Shop]([Id])
		ON DELETE NO ACTION
		ON UPDATE NO ACTION
go


ALTER TABLE [Shop]
	ADD CONSTRAINT [R_1] FOREIGN KEY ([IdCity]) REFERENCES [City]([Id])
		ON DELETE NO ACTION
		ON UPDATE NO ACTION
go


ALTER TABLE [Transaction]
	ADD CONSTRAINT [R_6] FOREIGN KEY ([IdOrder]) REFERENCES [Order]([Id])
		ON DELETE NO ACTION
		ON UPDATE NO ACTION
go

ALTER TABLE [Transaction]
	ADD CONSTRAINT [R_13] FOREIGN KEY ([IdShop]) REFERENCES [Shop]([Id])
		ON DELETE NO ACTION
		ON UPDATE NO ACTION
go

ALTER TABLE [Transaction]
	ADD CONSTRAINT [R_17] FOREIGN KEY ([IdBuyer]) REFERENCES [Buyer]([Id])
		ON DELETE NO ACTION
		ON UPDATE NO ACTION
go

/****** Object:  StoredProcedure [dbo].[SP_FINAL_PRICE] ******/
CREATE PROCEDURE [dbo].[SP_FINAL_PRICE]
	@IdOrder INT,
	@DiscountedPrice DECIMAL(10,3) OUTPUT
AS
BEGIN
	DECLARE @DateSent DATE, @IdBuyer INT

	DECLARE @TotalPrice DECIMAL(10,3)
	DECLARE @ItemQuantity INT, @ArticlePrice DECIMAL(10,3), @ArticleDiscount DECIMAL(10,3)

	DECLARE @HasTransactionExceeding10000 BIT

	--get information from the table 'Order'
	SELECT @DateSent = CONVERT(DATE, DateSent), @IdBuyer = IdBuyer
	FROM [Order]
	WHERE Id = @IdOrder

	--iterate through all items of the order, i.e., price, quantity, and discount for each item
	--total price of all order items, without any discounts
	--price of all order items, with discounts (initially, only with discounts from shops)
	SELECT @TotalPrice = SUM(OI.Quantity * P.Price),
		@DiscountedPrice = SUM(OI.Quantity * P.Price * (100 - S.Discount) / 100.0) --S.Discount is NOT NULL DEFAULT 0
	FROM OrderItem OI
		JOIN Article P ON (OI.IdArticle = P.Id)
		JOIN Shop S ON (P.IdShop = S.Id)
	WHERE OI.IdOrder = @IdOrder

	--check if we should apply an additional discount of 2%
	--if the buyer has made a purchase exceeding 10.000 in the previous 30 days
	IF EXISTS (
		SELECT 1
		FROM [Transaction]
		WHERE IdBuyer = @IdBuyer
			AND Amount > CAST(10000 AS DECIMAL(10,3))
			AND Date >= DATEADD(DAY, -30, @DateSent)
	)
		SET @HasTransactionExceeding10000 = 1; --true
	ELSE
		SET @HasTransactionExceeding10000 = 0; --false

	SELECT @DiscountedPrice = CASE
		WHEN (@HasTransactionExceeding10000 = 1) THEN (@DiscountedPrice * 0.98)
		ELSE @DiscountedPrice
	END

	--update the order with the prices and discounts
	UPDATE [Order]
	SET
		TotalPrice = @TotalPrice,
		DiscountedPrice = @DiscountedPrice,
		BuyerDiscount = CASE
			WHEN (@HasTransactionExceeding10000 = 1) THEN 2
			ELSE 0
		END
	WHERE Id = @IdOrder;
END
go

/****** Object:  Trigger [dbo].[TR_TRANSFER_MONEY_TO_SHOPS] ******/
CREATE TRIGGER [dbo].[TR_TRANSFER_MONEY_TO_SHOPS]
   ON [dbo].[Order]
   AFTER UPDATE
AS 
BEGIN
	/*
	--In this case, it is not smart to use this approach
	--because the UPDATE() will return true if any change occurs in the 'Status' column,
	--but it does not guarantee that the change occurred specifically to the value 'arrived'.
	IF UPDATE(Status) AND EXISTS (SELECT * FROM inserted WHERE Status = 'arrived')
	BEGIN
		...
	END
	*/

	DECLARE @Cursor1 CURSOR, @Cursor2 CURSOR
	DECLARE @IdOrder INT, @StatusNew VARCHAR(100), @StatusOld VARCHAR(100)
	DECLARE @BuyerDiscount INT

	DECLARE @IdShop INT, @ShopItemsPrice DECIMAL(10,3)

	--we will compare if there has been a change in the 'Status' to 'arrived'
	SET @Cursor1 = CURSOR FOR
	SELECT I.Id, I.Status AS 'StatusNew', D.Status AS 'StatusOld', I.BuyerDiscount
	FROM inserted I JOIN deleted D ON (I.Id = D.Id)

	OPEN @Cursor1

	FETCH NEXT FROM @Cursor1
	INTO @IdOrder, @StatusNew, @StatusOld, @BuyerDiscount

	WHILE @@FETCH_STATUS = 0
	BEGIN
		if (@StatusOld != @StatusNew AND @StatusNew = 'arrived')
		BEGIN
			--join the amount paid by the buyer to each shop with the shop
			SET @Cursor2 = CURSOR FOR
			SELECT S.Id, SUM(OI.Quantity * P.Price * (100 - S.Discount) / 100.0)
			FROM OrderItem OI
				JOIN Article P ON (OI.IdArticle = P.Id)
				JOIN Shop S ON (P.IdShop = S.Id)
			WHERE OI.IdOrder = @IdOrder
			GROUP BY S.Id

			OPEN @Cursor2

			FETCH NEXT FROM @Cursor2
			INTO @IdShop, @ShopItemsPrice

			WHILE @@FETCH_STATUS = 0
			BEGIN
				--update the shop's account balance
				UPDATE Shop
				SET
					Balance = Balance + CASE
						WHEN (@BuyerDiscount = 0) THEN @ShopItemsPrice * 0.95
						ELSE @ShopItemsPrice * 0.97
					END
				WHERE Id = @IdShop

				--create a new transaction
				INSERT INTO [dbo].[Transaction] (Date, Amount, IdOrder, IdShop, IdBuyer)
				VALUES ((SELECT DateArrived FROM inserted WHERE Id = @IdOrder),
					CASE
						WHEN (@BuyerDiscount = 0) THEN @ShopItemsPrice * 0.95
						ELSE @ShopItemsPrice * 0.97
					END,
					@IdOrder, @IdShop, NULL
				)

				FETCH NEXT FROM @Cursor2
				INTO @IdShop, @ShopItemsPrice
			END

			CLOSE @Cursor2
			DEALLOCATE @Cursor2
		END

		FETCH NEXT FROM @Cursor1
		INTO @IdOrder, @StatusNew, @StatusOld, @BuyerDiscount
	END

	CLOSE @Cursor1
	DEALLOCATE @Cursor1
END
go