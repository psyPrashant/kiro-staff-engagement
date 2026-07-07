import { TestBed } from '@angular/core/testing';
import { User } from './user';

describe('User', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [User],
    }).compileComponents();
  });

  it('should create the component', () => {
    const fixture = TestBed.createComponent(User);
    expect(fixture.componentInstance).toBeTruthy();
  });
});
